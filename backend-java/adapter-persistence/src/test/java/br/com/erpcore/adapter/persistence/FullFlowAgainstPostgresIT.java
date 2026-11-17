package br.com.erpcore.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.erpcore.adapter.persistence.support.ApplicationRolePostgresTest;
import br.com.erpcore.adapter.persistence.support.PostgresTestDatabase;
import br.com.erpcore.application.identity.AuthenticatedPrincipal;
import br.com.erpcore.application.identity.CurrentContext;
import br.com.erpcore.application.identity.GetCurrentContextQuery;
import br.com.erpcore.application.identity.GetCurrentContextUseCase;
import br.com.erpcore.application.identity.LoginCommand;
import br.com.erpcore.application.identity.LoginUseCase;
import br.com.erpcore.application.identity.LogoutCommand;
import br.com.erpcore.application.identity.LogoutUseCase;
import br.com.erpcore.application.identity.RefreshSessionCommand;
import br.com.erpcore.application.identity.RefreshSessionUseCase;
import br.com.erpcore.application.identity.RegisterCompanyAndOwnerCommand;
import br.com.erpcore.application.identity.RegisterCompanyAndOwnerResult;
import br.com.erpcore.application.identity.RegisterCompanyAndOwnerUseCase;
import br.com.erpcore.application.identity.SessionTokens;
import br.com.erpcore.domain.identity.EmailAlreadyRegisteredException;
import br.com.erpcore.domain.identity.InvalidCredentialsException;
import br.com.erpcore.domain.identity.InvalidRefreshTokenException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Os cinco casos de uso de ponta a ponta sobre o PostgreSQL real, com o papel real da aplicação (sujeito a RLS).
 * Depois de cada passo confere o que realmente ficou gravado, lendo como superusuário.
 */
@ApplicationRolePostgresTest
class FullFlowAgainstPostgresIT {

    private static final String PASSWORD = "senha-sintetica-12345";
    private static final String WRONG_PASSWORD = "senha-errada-sintetica";

    @Autowired
    private RegisterCompanyAndOwnerUseCase register;
    @Autowired
    private LoginUseCase login;
    @Autowired
    private RefreshSessionUseCase refresh;
    @Autowired
    private LogoutUseCase logout;
    @Autowired
    private GetCurrentContextUseCase currentContext;

    private final JdbcClient truth = PostgresTestDatabase.superuserJdbc();
    private final String runId = UUID.randomUUID().toString();
    private final String email = "fluxo-" + runId + "@exemplo.test";

    private String correlation(String step) {
        return "fluxo-" + runId + "-" + step;
    }

    private List<Map<String, Object>> auditOf(String step) {
        return truth.sql("""
                SELECT event_type, outcome, company_id, actor_user_id, details ->> 'reason' AS reason
                  FROM audit_event WHERE correlation_id = :correlation
                """).param("correlation", correlation(step)).query().listOfRows();
    }

    private Map<String, Object> singleAudit(String step) {
        List<Map<String, Object>> events = auditOf(step);
        assertThat(events).as("auditoria do passo %s", step).hasSize(1);
        return events.get(0);
    }

    private RegisterCompanyAndOwnerResult registerOwner() {
        return register.execute(new RegisterCompanyAndOwnerCommand("Padaria Fictícia Ltda", "Ana Fictícia", email,
                PASSWORD, correlation("register")));
    }

    private SessionTokens loginAs(String password, String step) {
        return login.execute(new LoginCommand(email, password, correlation(step)));
    }

    private Map<String, Object> userRow() {
        return truth.sql("SELECT id, password_hash, failed_login_count, locked_until FROM \"user\" WHERE email = :email")
                .param("email", email).query().singleRow();
    }

    @Test
    void givenANewCompany_whenRegistering_thenCompanyOwnerMembershipAndAuditAreStoredWithoutThePlainPassword() {
        RegisterCompanyAndOwnerResult result = registerOwner();

        var membership = truth.sql("SELECT role FROM company_membership WHERE company_id = :c AND user_id = :u")
                .param("c", result.companyId()).param("u", result.userId()).query(String.class).single();
        assertThat(membership).isEqualTo("OWNER");
        assertThat((String) userRow().get("password_hash")).isNotEqualTo(PASSWORD).doesNotContain(PASSWORD);
        Map<String, Object> event = singleAudit("register");
        assertThat(event).containsEntry("event_type", "REGISTRATION").containsEntry("outcome", "SUCCESS")
                .containsEntry("company_id", result.companyId()).containsEntry("actor_user_id", result.userId());
    }

    @Test
    void givenAnEmailAlreadyRegistered_whenRegisteringAgain_thenItIsRejectedAndTheFailureIsAuditedAnyway() {
        registerOwner();

        assertThatThrownBy(() -> register.execute(new RegisterCompanyAndOwnerCommand("Outra Empresa Fictícia",
                "Beto Fictício", email.toUpperCase(), PASSWORD, correlation("duplicate"))))
                .isInstanceOf(EmailAlreadyRegisteredException.class);

        assertThat(truth.sql("SELECT count(*) FROM \"user\" WHERE email = :e").param("e", email)
                .query(Integer.class).single()).isEqualTo(1);
        assertThat(truth.sql("SELECT count(*) FROM company WHERE name = 'Outra Empresa Fictícia' AND created_at > now() - interval '1 minute'")
                .query(Integer.class).single()).isZero();
        Map<String, Object> event = singleAudit("duplicate");
        assertThat(event).containsEntry("outcome", "FAILURE").containsEntry("reason", "EMAIL_ALREADY_REGISTERED");
        assertThat(event.get("company_id")).isNull();
        assertThat(event.get("actor_user_id")).isNull();
    }

    @Test
    void givenCorrectCredentials_whenLoggingIn_thenOnlyTheHashOfTheRefreshTokenIsStored() {
        RegisterCompanyAndOwnerResult owner = registerOwner();

        SessionTokens tokens = loginAs(PASSWORD, "login");

        var stored = truth.sql("SELECT token_hash FROM refresh_token WHERE user_id = :u").param("u", owner.userId())
                .query(String.class).list();
        assertThat(stored).hasSize(1);
        assertThat(stored.get(0)).isNotEqualTo(tokens.refreshToken());
        assertThat(truth.sql("SELECT count(*) FROM refresh_token WHERE token_hash = :plain")
                .param("plain", tokens.refreshToken()).query(Integer.class).single()).isZero();
        Map<String, Object> event = singleAudit("login");
        assertThat(event).containsEntry("event_type", "LOGIN").containsEntry("outcome", "SUCCESS")
                .containsEntry("company_id", owner.companyId()).containsEntry("actor_user_id", owner.userId());
    }

    @Test
    void givenAWrongPasswordAndAnUnknownEmail_whenLoggingIn_thenBothFailTheSameWayButAreAuditedDifferently() {
        RegisterCompanyAndOwnerResult owner = registerOwner();

        Throwable wrong = org.assertj.core.api.Assertions.catchThrowable(() -> loginAs(WRONG_PASSWORD, "wrong"));
        Throwable unknown = org.assertj.core.api.Assertions.catchThrowable(() -> login.execute(
                new LoginCommand("ninguem-" + runId + "@exemplo.test", PASSWORD, correlation("unknown"))));

        assertThat(wrong).isInstanceOf(InvalidCredentialsException.class);
        assertThat(unknown).isInstanceOf(InvalidCredentialsException.class).hasMessage(wrong.getMessage());
        assertThat(userRow().get("failed_login_count")).isEqualTo(1);
        assertThat(singleAudit("wrong")).containsEntry("reason", "BAD_PASSWORD")
                .containsEntry("actor_user_id", owner.userId());
        Map<String, Object> unknownEvent = singleAudit("unknown");
        assertThat(unknownEvent).containsEntry("reason", "USER_NOT_FOUND");
        assertThat(unknownEvent.get("actor_user_id")).isNull();
    }

    @Test
    void givenRepeatedWrongPasswords_whenTheLimitIsReached_thenTheAccountLocksAndEvenTheRightPasswordIsRefused() {
        registerOwner();
        for (int attempt = 1; attempt <= 5; attempt++) {
            int number = attempt;
            assertThatThrownBy(() -> loginAs(WRONG_PASSWORD, "attempt-" + number))
                    .isInstanceOf(InvalidCredentialsException.class);
        }

        assertThatThrownBy(() -> loginAs(PASSWORD, "locked")).isInstanceOf(InvalidCredentialsException.class);

        assertThat(userRow().get("failed_login_count")).isEqualTo(5);
        assertThat(userRow().get("locked_until")).isNotNull();
        assertThat(singleAudit("locked")).containsEntry("reason", "LOCKED");
    }

    @Test
    void givenALoggedInUser_whenReadingTheContext_thenItShowsTheUserCompanyAndRole() {
        RegisterCompanyAndOwnerResult owner = registerOwner();
        loginAs(PASSWORD, "login");

        CurrentContext context = currentContext.execute(new GetCurrentContextQuery(
                new AuthenticatedPrincipal(owner.userId(), owner.companyId()), correlation("me")));

        assertThat(context.email()).isEqualTo(email);
        assertThat(context.displayName()).isEqualTo("Ana Fictícia");
        assertThat(context.companyName()).isEqualTo("Padaria Fictícia Ltda");
        assertThat(context.role()).isEqualTo("OWNER");
        assertThat(singleAudit("me")).containsEntry("event_type", "CONTEXT_VIEWED").containsEntry("outcome", "SUCCESS");
    }

    @Test
    void givenAValidRefreshToken_whenRefreshingAndThenReusingTheOldOne_thenTheWholeSessionIsRevoked() {
        RegisterCompanyAndOwnerResult owner = registerOwner();
        SessionTokens first = loginAs(PASSWORD, "login");

        SessionTokens second = refresh.execute(new RefreshSessionCommand(first.refreshToken(), correlation("refresh")));

        assertThat(second.refreshToken()).isNotEqualTo(first.refreshToken());
        assertThat(truth.sql("SELECT count(*) FROM refresh_token WHERE user_id = :u AND revoked_at IS NOT NULL")
                .param("u", owner.userId()).query(Integer.class).single()).isEqualTo(1);
        assertThat(singleAudit("refresh")).containsEntry("event_type", "TOKEN_REFRESH").containsEntry("outcome", "SUCCESS");

        assertThatThrownBy(() -> refresh.execute(new RefreshSessionCommand(first.refreshToken(), correlation("reuse"))))
                .isInstanceOf(InvalidRefreshTokenException.class);

        assertThat(truth.sql("SELECT count(*) FROM refresh_token WHERE user_id = :u AND revoked_at IS NULL")
                .param("u", owner.userId()).query(Integer.class).single()).isZero();
        assertThat(singleAudit("reuse")).containsEntry("event_type", "TOKEN_REUSE_DETECTED")
                .containsEntry("outcome", "FAILURE").containsEntry("company_id", owner.companyId());
        assertThatThrownBy(() -> refresh.execute(new RefreshSessionCommand(second.refreshToken(), correlation("after-reuse"))))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void givenALoggedInUser_whenLoggingOut_thenTheTokenIsRevokedAndCannotRefreshAnymore() {
        registerOwner();
        SessionTokens session = loginAs(PASSWORD, "login");

        logout.execute(new LogoutCommand(session.refreshToken(), correlation("logout")));

        assertThat(singleAudit("logout")).containsEntry("event_type", "LOGOUT").containsEntry("outcome", "SUCCESS");
        assertThatThrownBy(() -> refresh.execute(new RefreshSessionCommand(session.refreshToken(), correlation("late"))))
                .isInstanceOf(InvalidRefreshTokenException.class);
        logout.execute(new LogoutCommand("token-desconhecido-" + runId, correlation("unknown-logout")));
        assertThat(singleAudit("unknown-logout")).containsEntry("outcome", "FAILURE").containsEntry("reason", "UNKNOWN_TOKEN");
    }

    @Test
    void givenAWholeFlow_whenSearchingTheAuditTrail_thenNoPersonalDataOrSecretWasEverStored() {
        registerOwner();
        SessionTokens session = loginAs(PASSWORD, "login");
        assertThatThrownBy(() -> loginAs(WRONG_PASSWORD, "wrong")).isInstanceOf(InvalidCredentialsException.class);
        refresh.execute(new RefreshSessionCommand(session.refreshToken(), correlation("refresh")));

        var leaks = truth.sql("""
                SELECT count(*) FROM audit_event
                 WHERE correlation_id LIKE :prefix
                   AND (details::text LIKE '%' || :email || '%' OR details::text LIKE '%' || :password || '%'
                        OR details::text LIKE '%' || :token || '%' OR correlation_id LIKE '%' || :email || '%')
                """).param("prefix", "fluxo-" + runId + "-%").param("email", email).param("password", PASSWORD)
                .param("token", session.refreshToken()).query(Integer.class).single();

        assertThat(leaks).isZero();
        assertThat(truth.sql("SELECT count(*) FROM audit_event WHERE correlation_id LIKE :prefix")
                .param("prefix", "fluxo-" + runId + "-%").query(Integer.class).single()).isPositive();
    }
}
