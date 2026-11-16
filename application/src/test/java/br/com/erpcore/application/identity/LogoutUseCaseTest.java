package br.com.erpcore.application.identity;

import static br.com.erpcore.application.support.ApplicationFixture.CORRELATION_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.erpcore.application.shared.port.out.AccessScope;
import br.com.erpcore.application.support.ApplicationFixture;
import br.com.erpcore.domain.audit.AuditEvent;
import br.com.erpcore.domain.audit.AuditEventType;
import br.com.erpcore.domain.audit.AuditOutcome;
import br.com.erpcore.domain.company.CompanyId;
import br.com.erpcore.domain.identity.InvalidRefreshTokenException;
import br.com.erpcore.domain.identity.RefreshToken;
import br.com.erpcore.domain.identity.TokenHash;
import br.com.erpcore.domain.identity.UserId;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class LogoutUseCaseTest {

    private final ApplicationFixture fixture = new ApplicationFixture();
    private final RegisterCompanyAndOwnerResult owner = fixture.registerOwner();
    private final SessionTokens session = fixture.loginAsOwner();
    private final LogoutUseCase useCase = fixture.logoutUseCase();

    private LogoutCommand logout(String token) {
        return new LogoutCommand(token, CORRELATION_ID);
    }

    @Test
    void givenAValidRefreshToken_whenLoggingOut_thenTheTokenIsRevokedAndCannotRenewTheSession() {
        useCase.execute(logout(session.refreshToken()));

        assertThat(fixture.refreshTokens.all()).allMatch(RefreshToken::isRevoked);
        assertThatThrownBy(() -> fixture.refreshUseCase().execute(new RefreshSessionCommand(session.refreshToken(), CORRELATION_ID)))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void givenARenewedSession_whenLoggingOutWithTheLatestToken_thenTheWholeSessionEnds() {
        SessionTokens renewed = fixture.refreshUseCase()
                .execute(new RefreshSessionCommand(session.refreshToken(), CORRELATION_ID));

        useCase.execute(logout(renewed.refreshToken()));

        assertThat(fixture.refreshTokens.all()).hasSize(2).allMatch(RefreshToken::isRevoked);
    }

    @Test
    void givenAValidRefreshToken_whenLoggingOut_thenASuccessEventIsAuditedInARefreshTokenLookupTransaction() {
        useCase.execute(logout(session.refreshToken()));

        AuditEvent event = fixture.auditEvents.last();
        assertThat(event.type()).isEqualTo(AuditEventType.LOGOUT);
        assertThat(event.outcome()).isEqualTo(AuditOutcome.SUCCESS);
        assertThat(event.companyId()).contains(new CompanyId(owner.companyId()));
        assertThat(event.actorUserId()).contains(new UserId(owner.userId()));
        TokenHash expected = new TokenHash(fixture.refreshCodec.hash(session.refreshToken()));
        assertThat(fixture.unitOfWork.scopes().get(fixture.unitOfWork.scopes().size() - 1))
                .isEqualTo(new AccessScope.RefreshTokenLookup(expected));
    }

    @Test
    void givenATokenAlreadyLoggedOut_whenLoggingOutAgain_thenNothingFailsAndTheOriginalRevocationIsKept() {
        useCase.execute(logout(session.refreshToken()));
        var revokedAt = fixture.refreshTokens.all().get(0).revokedAt();
        fixture.clock.advance(Duration.ofHours(2));

        assertThatCode(() -> useCase.execute(logout(session.refreshToken()))).doesNotThrowAnyException();

        assertThat(fixture.refreshTokens.all().get(0).revokedAt()).isEqualTo(revokedAt);
        assertThat(fixture.auditEvents.last().outcome()).isEqualTo(AuditOutcome.SUCCESS);
    }

    @Test
    void givenAnUnknownToken_whenLoggingOut_thenNothingFailsButTheAttemptIsAudited() {
        assertThatCode(() -> useCase.execute(logout("token-que-nunca-existiu"))).doesNotThrowAnyException();

        AuditEvent event = fixture.auditEvents.last();
        assertThat(event.type()).isEqualTo(AuditEventType.LOGOUT);
        assertThat(event.outcome()).isEqualTo(AuditOutcome.FAILURE);
        assertThat(event.details()).containsEntry("reason", "UNKNOWN_TOKEN");
        assertThat(event.companyId()).isEmpty();
        assertThat(event.details().toString()).doesNotContain("token-que-nunca-existiu");
        assertThat(fixture.refreshTokens.all()).noneMatch(RefreshToken::isRevoked);
    }

    @Test
    void givenNoToken_whenLoggingOut_thenItIsTreatedAsUnknown() {
        assertThatCode(() -> useCase.execute(logout(null))).doesNotThrowAnyException();

        assertThat(fixture.auditEvents.last().details()).containsEntry("reason", "UNKNOWN_TOKEN");
    }

    @Test
    void givenACommand_whenPrintingIt_thenTheTokenDoesNotAppear() {
        assertThat(logout(session.refreshToken()).toString()).doesNotContain(session.refreshToken());
    }
}
