package br.com.erpcore.application.identity;

import static br.com.erpcore.application.support.ApplicationFixture.CORRELATION_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import br.com.erpcore.application.shared.port.out.AccessScope;
import br.com.erpcore.application.support.ApplicationFixture;
import br.com.erpcore.domain.audit.AuditEvent;
import br.com.erpcore.domain.audit.AuditEventType;
import br.com.erpcore.domain.audit.AuditOutcome;
import br.com.erpcore.domain.identity.InvalidRefreshTokenException;
import br.com.erpcore.domain.identity.RefreshToken;
import br.com.erpcore.domain.identity.RefreshTokenId;
import br.com.erpcore.domain.identity.TokenFamilyId;
import br.com.erpcore.domain.identity.TokenHash;
import br.com.erpcore.domain.identity.UserId;
import br.com.erpcore.domain.company.CompanyId;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RefreshSessionUseCaseTest {

    private final ApplicationFixture fixture = new ApplicationFixture();
    private final RegisterCompanyAndOwnerResult owner = fixture.registerOwner();
    private final SessionTokens firstSession = fixture.loginAsOwner();
    private final RefreshSessionUseCase useCase = fixture.refreshUseCase();

    private RefreshSessionCommand refresh(String token) {
        return new RefreshSessionCommand(token, CORRELATION_ID);
    }

    private AuditEvent lastAudit() {
        return fixture.auditEvents.last();
    }

    @Test
    void givenAValidRefreshToken_whenRefreshing_thenNewTokensAreIssued() {
        fixture.clock.advance(Duration.ofHours(1));

        SessionTokens renewed = useCase.execute(refresh(firstSession.refreshToken()));

        assertThat(renewed.refreshToken()).isNotEqualTo(firstSession.refreshToken());
        assertThat(renewed.accessToken()).isNotBlank();
        assertThat(renewed.refreshTokenExpiresAt()).isEqualTo(fixture.clock.instant().plus(Duration.ofDays(7)));
    }

    @Test
    void givenAValidRefreshToken_whenRefreshing_thenTheOldOneIsRevokedAndTheNewOneStaysInTheSameFamily() {
        useCase.execute(refresh(firstSession.refreshToken()));

        List<RefreshToken> tokens = fixture.refreshTokens.all();
        assertThat(tokens).hasSize(2);
        assertThat(tokens.get(0).isRevoked()).isTrue();
        assertThat(tokens.get(1).isRevoked()).isFalse();
        assertThat(tokens.get(1).familyId()).isEqualTo(tokens.get(0).familyId());
    }

    @Test
    void givenAValidRefreshToken_whenRefreshing_thenTheAccessTokenIsForTheSameUserAndCompany() {
        useCase.execute(refresh(firstSession.refreshToken()));

        assertThat(fixture.accessTokens.issuedFor()).hasSize(2);
        assertThat(fixture.accessTokens.issuedFor().get(1).userId()).isEqualTo(owner.userId());
        assertThat(fixture.accessTokens.issuedFor().get(1).companyId()).isEqualTo(owner.companyId());
        assertThat(fixture.accessTokens.issuedFor().get(1).role()).isEqualTo("OWNER");
    }

    @Test
    void givenAValidRefreshToken_whenRefreshing_thenASuccessEventIsAuditedInARefreshTokenLookupTransaction() {
        useCase.execute(refresh(firstSession.refreshToken()));

        AuditEvent event = lastAudit();
        assertThat(event.type()).isEqualTo(AuditEventType.TOKEN_REFRESH);
        assertThat(event.outcome()).isEqualTo(AuditOutcome.SUCCESS);
        assertThat(event.companyId()).contains(new CompanyId(owner.companyId()));
        assertThat(event.actorUserId()).contains(new UserId(owner.userId()));
        TokenHash expected = new TokenHash(fixture.refreshCodec.hash(firstSession.refreshToken()));
        assertThat(fixture.unitOfWork.scopes().get(fixture.unitOfWork.scopes().size() - 1))
                .isEqualTo(new AccessScope.RefreshTokenLookup(expected));
    }

    @Test
    void givenARotatedToken_whenItIsUsedAgain_thenItIsRejectedAndTheWholeSessionIsRevoked() {
        SessionTokens renewed = useCase.execute(refresh(firstSession.refreshToken()));

        assertThatThrownBy(() -> useCase.execute(refresh(firstSession.refreshToken())))
                .isInstanceOf(InvalidRefreshTokenException.class);

        assertThat(fixture.refreshTokens.all()).allMatch(RefreshToken::isRevoked);
        assertThatThrownBy(() -> useCase.execute(refresh(renewed.refreshToken())))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void givenReuseOfARotatedToken_whenRefreshing_thenTheReuseIsAuditedAndCommittedBeforeTheErrorIsRaised() {
        useCase.execute(refresh(firstSession.refreshToken()));

        catchThrowable(() -> useCase.execute(refresh(firstSession.refreshToken())));

        AuditEvent event = lastAudit();
        assertThat(event.type()).isEqualTo(AuditEventType.TOKEN_REUSE_DETECTED);
        assertThat(event.outcome()).isEqualTo(AuditOutcome.FAILURE);
        assertThat(event.companyId()).contains(new CompanyId(owner.companyId()));
        assertThat(event.actorUserId()).contains(new UserId(owner.userId()));
        assertThat(fixture.unitOfWork.rollbacks()).isZero();
    }

    @Test
    void givenAnUnknownToken_whenRefreshing_thenItIsRejectedAndAudited() {
        assertThatThrownBy(() -> useCase.execute(refresh("token-que-nunca-existiu")))
                .isInstanceOf(InvalidRefreshTokenException.class);

        AuditEvent event = lastAudit();
        assertThat(event.type()).isEqualTo(AuditEventType.TOKEN_REFRESH);
        assertThat(event.outcome()).isEqualTo(AuditOutcome.FAILURE);
        assertThat(event.details()).containsEntry("reason", "UNKNOWN_TOKEN");
        assertThat(event.companyId()).isEmpty();
        assertThat(event.details().toString()).doesNotContain("token-que-nunca-existiu");
    }

    @Test
    void givenNullOrBlankToken_whenRefreshing_thenItIsTreatedAsUnknown() {
        assertThat(catchThrowable(() -> useCase.execute(refresh(null)))).isInstanceOf(InvalidRefreshTokenException.class);
        assertThat(catchThrowable(() -> useCase.execute(refresh("  ")))).isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void givenAnExpiredToken_whenRefreshing_thenItIsRejectedAndAudited() {
        fixture.clock.advance(Duration.ofDays(7));

        assertThatThrownBy(() -> useCase.execute(refresh(firstSession.refreshToken())))
                .isInstanceOf(InvalidRefreshTokenException.class);

        assertThat(lastAudit().details()).containsEntry("reason", "EXPIRED");
        assertThat(fixture.refreshTokens.all()).hasSize(1);
    }

    @Test
    void givenASessionRenewedEverySixDays_whenItReachesThirtyDays_thenItEndsEvenThoughEachTokenWasRenewedInTime() {
        SessionTokens current = firstSession;
        for (int renewal = 1; renewal <= 4; renewal++) {
            fixture.clock.advance(Duration.ofDays(6));
            current = useCase.execute(refresh(current.refreshToken()));
        }
        SessionTokens lastAllowed = current;
        assertThat(lastAllowed.refreshTokenExpiresAt()).isEqualTo(ApplicationFixture.START.plus(Duration.ofDays(30)));

        fixture.clock.advance(Duration.ofDays(6));

        assertThatThrownBy(() -> useCase.execute(refresh(lastAllowed.refreshToken())))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void givenATokenWhoseUserLeftTheCompany_whenRefreshing_thenItIsRejectedAndTheSessionIsRevoked() {
        UserId orphan = fixture.seedUserWithoutMembership("sem.empresa@exemplo.test", "senha-sintetica-67890");
        String plain = "refresh-de-quem-saiu";
        fixture.unitOfWork.execute(new AccessScope.Anonymous(), () -> {
            fixture.refreshTokens.save(RefreshToken.first(new RefreshTokenId(UUID.fromString("00000000-0000-0000-0000-0000000000b1")),
                    orphan, new CompanyId(owner.companyId()), new TokenFamilyId(UUID.fromString("00000000-0000-0000-0000-0000000000b2")),
                    new TokenHash(fixture.refreshCodec.hash(plain)), fixture.clock.instant(), ApplicationFixture.SESSION_POLICY));
            return null;
        });

        assertThatThrownBy(() -> useCase.execute(refresh(plain))).isInstanceOf(InvalidRefreshTokenException.class);

        assertThat(lastAudit().details()).containsEntry("reason", "NO_MEMBERSHIP");
        assertThat(fixture.refreshTokens.all().stream().filter(token -> token.userId().equals(orphan)))
                .allMatch(RefreshToken::isRevoked);
    }

    @Test
    void givenACommand_whenPrintingIt_thenTheTokenDoesNotAppear() {
        assertThat(refresh(firstSession.refreshToken()).toString()).doesNotContain(firstSession.refreshToken());
    }
}
