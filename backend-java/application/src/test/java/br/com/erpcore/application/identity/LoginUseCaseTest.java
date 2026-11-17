package br.com.erpcore.application.identity;

import static br.com.erpcore.application.support.ApplicationFixture.CORRELATION_ID;
import static br.com.erpcore.application.support.ApplicationFixture.OWNER_EMAIL;
import static br.com.erpcore.application.support.ApplicationFixture.OWNER_PASSWORD;
import static br.com.erpcore.application.support.ApplicationFixture.WRONG_PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.erpcore.application.identity.port.out.AccessTokenSubject;
import br.com.erpcore.application.shared.port.out.AccessScope;
import br.com.erpcore.application.support.ApplicationFixture;
import br.com.erpcore.application.support.FakeAccessTokenIssuer;
import br.com.erpcore.domain.audit.AuditEvent;
import br.com.erpcore.domain.audit.AuditEventType;
import br.com.erpcore.domain.audit.AuditOutcome;
import br.com.erpcore.domain.company.CompanyId;
import br.com.erpcore.domain.identity.Email;
import br.com.erpcore.domain.identity.InvalidCredentialsException;
import br.com.erpcore.domain.identity.RefreshToken;
import br.com.erpcore.domain.identity.User;
import br.com.erpcore.domain.identity.UserId;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class LoginUseCaseTest {

    private final ApplicationFixture fixture = new ApplicationFixture();
    private final RegisterCompanyAndOwnerResult owner = fixture.registerOwner();
    private final LoginUseCase useCase = fixture.loginUseCase();

    private LoginCommand login(String email, String password) {
        return new LoginCommand(email, password, CORRELATION_ID);
    }

    private AuditEvent lastAudit() {
        return fixture.auditEvents.last();
    }

    @Test
    void givenCorrectCredentials_whenLoggingIn_thenAccessAndRefreshTokensAreIssuedWithTheirExpiries() {
        SessionTokens tokens = useCase.execute(login(OWNER_EMAIL, OWNER_PASSWORD));

        assertThat(tokens.accessToken()).isNotBlank();
        assertThat(tokens.accessTokenExpiresAt()).isEqualTo(ApplicationFixture.START.plus(FakeAccessTokenIssuer.LIFETIME));
        assertThat(tokens.refreshToken()).isNotBlank();
        assertThat(tokens.refreshTokenExpiresAt()).isEqualTo(ApplicationFixture.START.plus(Duration.ofDays(7)));
    }

    @Test
    void givenCorrectCredentials_whenLoggingIn_thenTheTokenIsForTheOwnerAndTheirCompanyFromTheMembership() {
        useCase.execute(login(OWNER_EMAIL, OWNER_PASSWORD));

        assertThat(fixture.accessTokens.issuedFor())
                .containsExactly(new AccessTokenSubject(owner.userId(), owner.companyId(), "OWNER"));
    }

    @Test
    void givenCorrectCredentials_whenLoggingIn_thenTheRefreshTokenIsStoredOnlyAsAHash() {
        SessionTokens tokens = useCase.execute(login(OWNER_EMAIL, OWNER_PASSWORD));

        RefreshToken stored = fixture.refreshTokens.all().get(0);
        assertThat(stored.tokenHash().value()).isEqualTo(fixture.refreshCodec.hash(tokens.refreshToken()));
        assertThat(stored.tokenHash().value()).isNotEqualTo(tokens.refreshToken());
        assertThat(stored.userId().value()).isEqualTo(owner.userId());
        assertThat(stored.companyId().value()).isEqualTo(owner.companyId());
    }

    @Test
    void givenCorrectCredentialsInOtherCase_whenLoggingIn_thenTheEmailIsMatchedIgnoringCase() {
        SessionTokens tokens = useCase.execute(login("ANA.Ficticia@EXEMPLO.test", OWNER_PASSWORD));

        assertThat(tokens.accessToken()).isNotBlank();
    }

    @Test
    void givenCorrectCredentials_whenLoggingIn_thenOneSuccessEventIsAuditedInACredentialLookupTransaction() {
        useCase.execute(login(OWNER_EMAIL, OWNER_PASSWORD));

        AuditEvent event = lastAudit();
        assertThat(event.type()).isEqualTo(AuditEventType.LOGIN);
        assertThat(event.outcome()).isEqualTo(AuditOutcome.SUCCESS);
        assertThat(event.companyId()).contains(new CompanyId(owner.companyId()));
        assertThat(event.actorUserId()).contains(new UserId(owner.userId()));
        assertThat(event.correlationId()).isEqualTo(CORRELATION_ID);
        assertThat(fixture.unitOfWork.scopes().get(1)).isEqualTo(new AccessScope.CredentialLookup(Email.of(OWNER_EMAIL)));
    }

    @Test
    void givenTwoLogins_whenComparingTheirSessions_thenEachStartsItsOwnTokenFamily() {
        useCase.execute(login(OWNER_EMAIL, OWNER_PASSWORD));
        useCase.execute(login(OWNER_EMAIL, OWNER_PASSWORD));

        var tokens = fixture.refreshTokens.all();
        assertThat(tokens.get(0).familyId()).isNotEqualTo(tokens.get(1).familyId());
    }

    @Test
    void givenAWrongPassword_whenLoggingIn_thenItIsRejectedWithTheGenericError() {
        assertThatThrownBy(() -> useCase.execute(login(OWNER_EMAIL, WRONG_PASSWORD)))
                .isInstanceOf(InvalidCredentialsException.class);

        assertThat(fixture.refreshTokens.all()).isEmpty();
    }

    @Test
    void givenAWrongPassword_whenLoggingIn_thenTheFailureIsCountedAndAuditedAndCommittedBeforeTheErrorIsRaised() {
        assertThatThrownBy(() -> useCase.execute(login(OWNER_EMAIL, WRONG_PASSWORD)))
                .isInstanceOf(InvalidCredentialsException.class);

        User user = fixture.users.all().get(0);
        assertThat(user.failedLoginCount()).isEqualTo(1);
        AuditEvent event = lastAudit();
        assertThat(event.type()).isEqualTo(AuditEventType.LOGIN);
        assertThat(event.outcome()).isEqualTo(AuditOutcome.FAILURE);
        assertThat(event.details()).containsEntry("reason", "BAD_PASSWORD");
        assertThat(event.actorUserId()).contains(user.id());
        assertThat(fixture.unitOfWork.rollbacks()).isZero();
    }

    @Test
    void givenAnUnknownEmail_whenLoggingIn_thenItIsRejectedAndTimeIsSpentAsIfThePasswordWasChecked() {
        assertThatThrownBy(() -> useCase.execute(login("ninguem@exemplo.test", OWNER_PASSWORD)))
                .isInstanceOf(InvalidCredentialsException.class);

        assertThat(fixture.passwordHasher.placeholderVerifications()).isEqualTo(1);
        AuditEvent event = lastAudit();
        assertThat(event.outcome()).isEqualTo(AuditOutcome.FAILURE);
        assertThat(event.details()).containsEntry("reason", "USER_NOT_FOUND");
        assertThat(event.actorUserId()).isEmpty();
        assertThat(event.details().toString()).doesNotContain("ninguem");
    }

    @Test
    void givenUnknownUserAndWrongPassword_whenComparingTheErrors_thenTheyAreIndistinguishable() {
        Throwable unknownUser = catchFailure(login("ninguem@exemplo.test", OWNER_PASSWORD));
        Throwable wrongPassword = catchFailure(login(OWNER_EMAIL, WRONG_PASSWORD));

        assertThat(unknownUser).isExactlyInstanceOf(wrongPassword.getClass());
        assertThat(unknownUser.getMessage()).isEqualTo(wrongPassword.getMessage());
    }

    @Test
    void givenRepeatedWrongPasswords_whenTheThresholdIsReached_thenEvenTheCorrectPasswordIsRefusedWhileLocked() {
        for (int attempt = 0; attempt < 3; attempt++) {
            catchFailure(login(OWNER_EMAIL, WRONG_PASSWORD));
        }
        int placeholderBefore = fixture.passwordHasher.placeholderVerifications();

        Throwable whileLocked = catchFailure(login(OWNER_EMAIL, OWNER_PASSWORD));

        assertThat(whileLocked).isInstanceOf(InvalidCredentialsException.class);
        assertThat(lastAudit().details()).containsEntry("reason", "LOCKED");
        assertThat(fixture.passwordHasher.placeholderVerifications()).isEqualTo(placeholderBefore + 1);
        assertThat(fixture.refreshTokens.all()).isEmpty();
    }

    @Test
    void givenALockThatExpired_whenLoggingInWithTheCorrectPassword_thenItWorksAndTheFailuresAreCleared() {
        for (int attempt = 0; attempt < 3; attempt++) {
            catchFailure(login(OWNER_EMAIL, WRONG_PASSWORD));
        }
        fixture.clock.advance(Duration.ofMinutes(1));

        SessionTokens tokens = useCase.execute(login(OWNER_EMAIL, OWNER_PASSWORD));

        assertThat(tokens.accessToken()).isNotBlank();
        assertThat(fixture.users.all().get(0).failedLoginCount()).isZero();
        assertThat(fixture.users.all().get(0).lockedUntil()).isEmpty();
    }

    @Test
    void givenSomeFailures_whenLoggingInCorrectly_thenTheFailureCounterIsReset() {
        catchFailure(login(OWNER_EMAIL, WRONG_PASSWORD));
        catchFailure(login(OWNER_EMAIL, WRONG_PASSWORD));

        useCase.execute(login(OWNER_EMAIL, OWNER_PASSWORD));

        assertThat(fixture.users.all().get(0).failedLoginCount()).isZero();
    }

    @Test
    void givenMalformedInput_whenLoggingIn_thenItIsRejectedAsInvalidCredentialsNotAsAValidationError() {
        assertThat(catchFailure(login("isso-nao-e-email", OWNER_PASSWORD))).isInstanceOf(InvalidCredentialsException.class);
        assertThat(catchFailure(login(OWNER_EMAIL, ""))).isInstanceOf(InvalidCredentialsException.class);
        assertThat(catchFailure(login(null, null))).isInstanceOf(InvalidCredentialsException.class);

        assertThat(fixture.passwordHasher.placeholderVerifications()).isEqualTo(3);
        AuditEvent event = lastAudit();
        assertThat(event.details()).containsEntry("reason", "INVALID_INPUT");
        assertThat(fixture.unitOfWork.scopes().get(1)).isEqualTo(new AccessScope.Anonymous());
        assertThat(fixture.users.all().get(0).failedLoginCount()).isZero();
    }

    @Test
    void givenAUserWithoutAnyCompany_whenLoggingInWithTheCorrectPassword_thenItIsRejectedAndNoSessionStarts() {
        fixture.seedUserWithoutMembership("sem.empresa@exemplo.test", "senha-sintetica-67890");

        Throwable failure = catchFailure(login("sem.empresa@exemplo.test", "senha-sintetica-67890"));

        assertThat(failure).isInstanceOf(InvalidCredentialsException.class);
        assertThat(lastAudit().details()).containsEntry("reason", "NO_MEMBERSHIP");
        assertThat(fixture.refreshTokens.all()).isEmpty();
    }

    @Test
    void givenACommand_whenPrintingIt_thenNeitherThePasswordNorTheEmailAppears() {
        String printed = login(OWNER_EMAIL, OWNER_PASSWORD).toString();

        assertThat(printed).doesNotContain(OWNER_PASSWORD).doesNotContain(OWNER_EMAIL);
    }

    @Test
    void givenIssuedTokens_whenPrintingThem_thenNoTokenAppears() {
        SessionTokens tokens = useCase.execute(login(OWNER_EMAIL, OWNER_PASSWORD));

        assertThat(tokens.toString()).doesNotContain(tokens.accessToken()).doesNotContain(tokens.refreshToken());
    }

    private Throwable catchFailure(LoginCommand command) {
        return org.assertj.core.api.Assertions.catchThrowable(() -> useCase.execute(command));
    }
}
