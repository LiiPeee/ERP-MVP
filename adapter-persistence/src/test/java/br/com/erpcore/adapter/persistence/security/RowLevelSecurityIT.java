package br.com.erpcore.adapter.persistence.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.erpcore.adapter.persistence.support.ApplicationRolePostgresTest;
import br.com.erpcore.adapter.persistence.support.PostgresTestDatabase;
import br.com.erpcore.application.company.port.out.CompanyRepository;
import br.com.erpcore.application.identity.GetCurrentContextQuery;
import br.com.erpcore.application.identity.GetCurrentContextUseCase;
import br.com.erpcore.application.identity.AuthenticatedPrincipal;
import br.com.erpcore.application.identity.LoginCommand;
import br.com.erpcore.application.identity.LoginUseCase;
import br.com.erpcore.application.identity.RegisterCompanyAndOwnerCommand;
import br.com.erpcore.application.identity.RegisterCompanyAndOwnerUseCase;
import br.com.erpcore.application.identity.SessionTokens;
import br.com.erpcore.application.identity.port.out.MembershipRepository;
import br.com.erpcore.application.identity.port.out.RefreshTokenRepository;
import br.com.erpcore.application.identity.port.out.UserRepository;
import br.com.erpcore.application.shared.port.out.AccessScope;
import br.com.erpcore.application.shared.port.out.UnitOfWork;
import br.com.erpcore.application.support.FakeRefreshTokenCodec;
import br.com.erpcore.domain.company.CompanyId;
import br.com.erpcore.domain.identity.AccessDeniedException;
import br.com.erpcore.domain.identity.Email;
import br.com.erpcore.domain.identity.TokenHash;
import br.com.erpcore.domain.identity.UserId;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.TestPropertySource;

/**
 * Duas empresas no mesmo banco e um usuário de cada. Prova, com o papel real da aplicação, que uma empresa não
 * lê, não altera e não apaga o que é da outra, nem por identificador direto (IDOR), em nenhum escopo de acesso.
 * Todas as tentativas de vazamento acontecem depois de confirmar, como superusuário, que o dado alheio existe.
 */
@ApplicationRolePostgresTest
@TestPropertySource(properties = "spring.datasource.hikari.maximum-pool-size=1")
class RowLevelSecurityIT {

    private static final String PASSWORD = "senha-sintetica-12345";

    @Autowired
    private UnitOfWork unitOfWork;
    @Autowired
    private JdbcClient jdbc;
    @Autowired
    private UserRepository users;
    @Autowired
    private CompanyRepository companies;
    @Autowired
    private MembershipRepository memberships;
    @Autowired
    private RefreshTokenRepository refreshTokens;
    @Autowired
    private RegisterCompanyAndOwnerUseCase registerUseCase;
    @Autowired
    private LoginUseCase loginUseCase;
    @Autowired
    private GetCurrentContextUseCase currentContextUseCase;
    @Autowired
    private FakeRefreshTokenCodec refreshCodec;

    private Account ana;
    private Account beto;
    private SessionTokens anaSession;
    private SessionTokens betoSession;

    @BeforeEach
    void twoCompaniesEachWithAnOwnerAndASession() {
        ana = register("ana");
        beto = register("beto");
        anaSession = login(ana);
        betoSession = login(beto);
    }

    private record Account(String email, UUID companyId, UUID userId) {
        CompanyId company() {
            return new CompanyId(companyId);
        }

        UserId user() {
            return new UserId(userId);
        }
    }

    private Account register(String label) {
        String email = label + "-" + UUID.randomUUID() + "@exemplo.test";
        var result = registerUseCase.execute(new RegisterCompanyAndOwnerCommand("Empresa Fictícia " + label,
                "Dono Fictício " + label, email, PASSWORD, "correlacao-sintetica"));
        return new Account(email, result.companyId(), result.userId());
    }

    private SessionTokens login(Account account) {
        return loginUseCase.execute(new LoginCommand(account.email(), PASSWORD, "correlacao-sintetica"));
    }

    private AccessScope.Tenant asTenant(Account account) {
        return new AccessScope.Tenant(account.user(), account.company());
    }

    private AccessScope.RefreshTokenLookup lookupOf(SessionTokens session) {
        return new AccessScope.RefreshTokenLookup(new TokenHash(refreshCodec.hash(session.refreshToken())));
    }

    private <T> T inScope(AccessScope scope, Supplier<T> work) {
        return unitOfWork.execute(scope, work);
    }

    /** Conta linhas como superusuário (que ignora RLS): é a verdade do banco. */
    private int actualRows(String sql, Object value) {
        return PostgresTestDatabase.superuserJdbc().sql(sql).param(value).query(Integer.class).single();
    }

    // ---------- Pré-condição: o dado da outra empresa existe de verdade ----------

    @Test
    void givenTwoCompanies_whenCountingAsSuperuser_thenTheOtherCompanysDataReallyExists() {
        assertThat(actualRows("SELECT count(*) FROM company WHERE id = ?", beto.companyId())).isEqualTo(1);
        assertThat(actualRows("SELECT count(*) FROM \"user\" WHERE id = ?", beto.userId())).isEqualTo(1);
        assertThat(actualRows("SELECT count(*) FROM company_membership WHERE company_id = ?", beto.companyId()))
                .isEqualTo(1);
        assertThat(actualRows("SELECT count(*) FROM refresh_token WHERE company_id = ?", beto.companyId()))
                .isEqualTo(1);
    }

    // ---------- Leitura ----------

    @Test
    void givenATenantScope_whenReadingItsOwnData_thenEverythingIsVisible() {
        inScope(asTenant(ana), () -> {
            assertThat(companies.findById(ana.company())).isPresent();
            assertThat(users.findById(ana.user())).isPresent();
            assertThat(memberships.find(ana.company(), ana.user())).isPresent();
            return null;
        });
    }

    @Test
    void givenATenantScope_whenReadingAnotherCompanyByItsId_thenNothingIsVisible() {
        inScope(asTenant(ana), () -> {
            assertThat(companies.findById(beto.company())).isEmpty();
            assertThat(users.findById(beto.user())).isEmpty();
            assertThat(memberships.find(beto.company(), beto.user())).isEmpty();
            assertThat(memberships.findByUserId(beto.user())).isEmpty();
            return null;
        });
    }

    @Test
    void givenATenantScope_whenListingEveryRowOfEachTable_thenOnlyItsOwnAppear() {
        inScope(asTenant(ana), () -> {
            assertThat(jdbc.sql("SELECT id FROM company").query(UUID.class).list()).containsExactly(ana.companyId());
            assertThat(jdbc.sql("SELECT id FROM \"user\"").query(UUID.class).list()).containsExactly(ana.userId());
            assertThat(jdbc.sql("SELECT company_id FROM company_membership").query(UUID.class).list())
                    .containsOnly(ana.companyId());
            assertThat(jdbc.sql("SELECT count(*) FROM refresh_token").query(Integer.class).single()).isZero();
            return null;
        });
    }

    @Test
    void givenAForgedPrincipalOfAnotherCompany_whenReadingTheContext_thenAccessIsDeniedByTheUseCaseAndTheDatabase() {
        assertThatThrownBy(() -> currentContextUseCase.execute(new GetCurrentContextQuery(
                new AuthenticatedPrincipal(ana.userId(), beto.companyId()), "correlacao-sintetica")))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> currentContextUseCase.execute(new GetCurrentContextQuery(
                new AuthenticatedPrincipal(beto.userId(), ana.companyId()), "correlacao-sintetica")))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void givenACredentialLookupOfOneEmail_whenReadingAnotherAccount_thenItIsInvisible() {
        inScope(new AccessScope.CredentialLookup(Email.of(ana.email())), () -> {
            assertThat(users.findByEmail(Email.of(ana.email()))).isPresent();
            assertThat(users.findByEmail(Email.of(beto.email()))).isEmpty();
            assertThat(users.findById(beto.user())).isEmpty();
            assertThat(memberships.findByUserId(beto.user())).isEmpty();
            assertThat(memberships.findByUserId(ana.user())).hasSize(1);
            return null;
        });
    }

    @Test
    void givenARefreshTokenLookup_whenReadingAnotherSessionOrAccount_thenItIsInvisible() {
        inScope(lookupOf(anaSession), () -> {
            assertThat(refreshTokens.findByHash(new TokenHash(refreshCodec.hash(anaSession.refreshToken())))).isPresent();
            assertThat(refreshTokens.findByHash(new TokenHash(refreshCodec.hash(betoSession.refreshToken())))).isEmpty();
            assertThat(memberships.find(beto.company(), beto.user())).isEmpty();
            assertThat(memberships.find(ana.company(), ana.user())).isPresent();
            return null;
        });
    }

    @Test
    void givenARegistrationScope_whenReadingExistingAccounts_thenOnlyTheEmailBeingRegisteredIsVisible() {
        var scope = new AccessScope.Registration(new CompanyId(UUID.randomUUID()), Email.of(ana.email()));

        inScope(scope, () -> {
            assertThat(users.findByEmail(Email.of(ana.email()))).isPresent();
            assertThat(users.findById(beto.user())).isEmpty();
            assertThat(companies.findById(ana.company())).isEmpty();
            assertThat(memberships.find(ana.company(), ana.user())).isEmpty();
            return null;
        });
    }

    @Test
    void givenNoScopeAtAll_whenReadingAnyTable_thenNoRowIsVisible() {
        assertThat(jdbc.sql("SELECT count(*) FROM company").query(Integer.class).single()).isZero();
        assertThat(jdbc.sql("SELECT count(*) FROM \"user\"").query(Integer.class).single()).isZero();
        assertThat(jdbc.sql("SELECT count(*) FROM company_membership").query(Integer.class).single()).isZero();
        assertThat(jdbc.sql("SELECT count(*) FROM refresh_token").query(Integer.class).single()).isZero();
    }

    @Test
    void givenAnAnonymousScope_whenReadingAnyTable_thenNoRowIsVisible() {
        inScope(new AccessScope.Anonymous(), () -> {
            assertThat(jdbc.sql("SELECT count(*) FROM company").query(Integer.class).single()).isZero();
            assertThat(jdbc.sql("SELECT count(*) FROM \"user\"").query(Integer.class).single()).isZero();
            assertThat(jdbc.sql("SELECT count(*) FROM company_membership").query(Integer.class).single()).isZero();
            return null;
        });
    }

    @Test
    void givenAScopeThatEnded_whenTheSameConnectionIsUsedAgain_thenTheScopeDidNotLeak() {
        inScope(asTenant(ana), () -> users.findById(ana.user()));

        assertThat(jdbc.sql("SELECT coalesce(current_setting('app.scope', true), '')").query(String.class).single())
                .isEmpty();
        assertThat(jdbc.sql("SELECT count(*) FROM \"user\"").query(Integer.class).single()).isZero();
    }

    @Test
    void givenTheTableOwnerWithoutAScope_whenReadingAnyTable_thenForcedRowSecurityHidesEverything() {
        var owner = PostgresTestDatabase.migratorJdbc();

        assertThat(owner.sql("SELECT count(*) FROM company").query(Integer.class).single()).isZero();
        assertThat(owner.sql("SELECT count(*) FROM \"user\"").query(Integer.class).single()).isZero();
        assertThat(owner.sql("SELECT count(*) FROM refresh_token").query(Integer.class).single()).isZero();
    }

    // ---------- Alteração ----------

    @Test
    void givenACredentialLookupOfOneEmail_whenUpdatingAnotherUser_thenNoRowChanges() {
        inScope(new AccessScope.CredentialLookup(Email.of(ana.email())), () -> {
            int others = jdbc.sql("UPDATE \"user\" SET failed_login_count = 99 WHERE id = :id")
                    .param("id", beto.userId()).update();
            int own = jdbc.sql("UPDATE \"user\" SET failed_login_count = 1 WHERE id = :id")
                    .param("id", ana.userId()).update();
            assertThat(others).isZero();
            assertThat(own).isEqualTo(1);
            return null;
        });

        assertThat(actualRows("SELECT failed_login_count FROM \"user\" WHERE id = ?", beto.userId())).isZero();
    }

    @Test
    void givenATenantScope_whenUpdatingAnyUser_thenNoRowChanges() {
        inScope(asTenant(ana), () -> {
            assertThat(jdbc.sql("UPDATE \"user\" SET failed_login_count = 99 WHERE id = :id")
                    .param("id", beto.userId()).update()).isZero();
            assertThat(jdbc.sql("UPDATE \"user\" SET failed_login_count = 99 WHERE id = :id")
                    .param("id", ana.userId()).update()).isZero();
            return null;
        });

        assertThat(actualRows("SELECT failed_login_count FROM \"user\" WHERE id = ?", ana.userId())).isZero();
    }

    @Test
    void givenARefreshTokenLookup_whenRevokingAnotherSession_thenItStaysActive() {
        inScope(lookupOf(anaSession), () -> {
            refreshTokens.findByHash(new TokenHash(refreshCodec.hash(anaSession.refreshToken())));
            int others = jdbc.sql("UPDATE refresh_token SET revoked_at = now() WHERE company_id = :id")
                    .param("id", beto.companyId()).update();
            assertThat(others).isZero();
            return null;
        });

        assertThat(actualRows("SELECT count(*) FROM refresh_token WHERE company_id = ? AND revoked_at IS NOT NULL",
                beto.companyId())).isZero();
    }

    @Test
    void givenAnyScope_whenChangingColumnsTheApplicationMayNotTouch_thenThePrivilegeIsDenied() {
        assertThatThrownBy(() -> inScope(new AccessScope.CredentialLookup(Email.of(ana.email())),
                () -> jdbc.sql("UPDATE \"user\" SET password_hash = 'x' WHERE id = :id")
                        .param("id", ana.userId()).update()))
                .isInstanceOf(DataAccessException.class).rootCause().hasMessageContaining("permission denied");
        assertThatThrownBy(() -> inScope(lookupOf(anaSession),
                () -> jdbc.sql("UPDATE refresh_token SET expires_at = now() + interval '10 years'").update()))
                .isInstanceOf(DataAccessException.class).rootCause().hasMessageContaining("permission denied");
    }

    // ---------- Remoção ----------

    @Test
    void givenAnyScope_whenDeletingAnything_thenThePrivilegeIsDeniedAndTheDataSurvives() {
        for (String table : new String[] {"company", "\"user\"", "company_membership", "refresh_token", "audit_event"}) {
            assertThatThrownBy(() -> inScope(asTenant(beto), () -> jdbc.sql("DELETE FROM " + table).update()))
                    .as("DELETE on %s", table)
                    .isInstanceOf(DataAccessException.class).rootCause().hasMessageContaining("permission denied");
        }

        assertThat(actualRows("SELECT count(*) FROM company WHERE id = ?", beto.companyId())).isEqualTo(1);
    }

    // ---------- Escrita em outra empresa ----------

    @Test
    void givenATenantScope_whenInsertingAMembershipInAnotherCompany_thenRowSecurityRejectsIt() {
        assertThatThrownBy(() -> inScope(asTenant(ana), () -> jdbc.sql("""
                INSERT INTO company_membership (id, company_id, user_id, role, created_at)
                VALUES (gen_random_uuid(), :company, :user, 'OWNER', now())
                """).param("company", beto.companyId()).param("user", ana.userId()).update()))
                .isInstanceOf(DataAccessException.class).rootCause().hasMessageContaining("row-level security");

        assertThat(actualRows("SELECT count(*) FROM company_membership WHERE company_id = ?", beto.companyId()))
                .isEqualTo(1);
    }

    @Test
    void givenATenantScope_whenInsertingAnyMembershipOrToken_thenRowSecurityRejectsIt() {
        assertThatThrownBy(() -> inScope(asTenant(ana), () -> jdbc.sql("""
                INSERT INTO company_membership (id, company_id, user_id, role, created_at)
                VALUES (gen_random_uuid(), :company, :user, 'OWNER', now())
                """).param("company", ana.companyId()).param("user", beto.userId()).update()))
                .isInstanceOf(DataAccessException.class).rootCause().hasMessageContaining("row-level security");
        assertThatThrownBy(() -> inScope(asTenant(ana), () -> jdbc.sql("""
                INSERT INTO refresh_token (id, user_id, company_id, family_id, token_hash, issued_at, expires_at,
                                           family_started_at)
                VALUES (gen_random_uuid(), :user, :company, gen_random_uuid(), 'hash-forjado', now(), now(), now())
                """).param("user", ana.userId()).param("company", ana.companyId()).update()))
                .isInstanceOf(DataAccessException.class).rootCause().hasMessageContaining("row-level security");
    }

    @Test
    void givenARegistrationScope_whenInsertingIntoAnExistingCompany_thenRowSecurityRejectsIt() {
        var scope = new AccessScope.Registration(new CompanyId(UUID.randomUUID()), Email.of("novo-" + UUID.randomUUID() + "@exemplo.test"));

        assertThatThrownBy(() -> inScope(scope, () -> jdbc.sql("""
                INSERT INTO company_membership (id, company_id, user_id, role, created_at)
                VALUES (gen_random_uuid(), :company, :user, 'OWNER', now())
                """).param("company", ana.companyId()).param("user", beto.userId()).update()))
                .isInstanceOf(DataAccessException.class).rootCause().hasMessageContaining("row-level security");
        assertThatThrownBy(() -> inScope(scope, () -> jdbc.sql(
                "INSERT INTO company (id, name, created_at) VALUES (gen_random_uuid(), 'Outra', now())").update()))
                .isInstanceOf(DataAccessException.class).rootCause().hasMessageContaining("row-level security");
    }

    // ---------- Auditoria ----------

    @Test
    void givenAnyScope_whenReadingTheAuditTrail_thenThePrivilegeIsDenied() {
        assertThatThrownBy(() -> inScope(asTenant(ana), () -> jdbc.sql("SELECT count(*) FROM audit_event")
                .query(Integer.class).single()))
                .isInstanceOf(DataAccessException.class).rootCause().hasMessageContaining("permission denied");
    }

    @Test
    void givenATenantScope_whenWritingAnAuditEventOfAnotherCompany_thenRowSecurityRejectsIt() {
        assertThatThrownBy(() -> inScope(asTenant(ana), () -> jdbc.sql("""
                INSERT INTO audit_event (id, occurred_at, company_id, actor_user_id, event_type, outcome, correlation_id)
                VALUES (gen_random_uuid(), now(), :company, :actor, 'LOGIN', 'SUCCESS', 'forjado')
                """).param("company", beto.companyId()).param("actor", ana.userId()).update()))
                .isInstanceOf(DataAccessException.class).rootCause().hasMessageContaining("row-level security");
    }

    @Test
    void givenAnAnonymousScope_whenWritingAnAuditEventTiedToACompanyOrUser_thenRowSecurityRejectsIt() {
        assertThatThrownBy(() -> inScope(new AccessScope.Anonymous(), () -> jdbc.sql("""
                INSERT INTO audit_event (id, occurred_at, company_id, event_type, outcome, correlation_id)
                VALUES (gen_random_uuid(), now(), :company, 'LOGIN', 'FAILURE', 'forjado')
                """).param("company", ana.companyId()).update()))
                .isInstanceOf(DataAccessException.class).rootCause().hasMessageContaining("row-level security");
        assertThatThrownBy(() -> inScope(new AccessScope.Anonymous(), () -> jdbc.sql("""
                INSERT INTO audit_event (id, occurred_at, actor_user_id, event_type, outcome, correlation_id)
                VALUES (gen_random_uuid(), now(), :actor, 'LOGIN', 'FAILURE', 'forjado')
                """).param("actor", ana.userId()).update()))
                .isInstanceOf(DataAccessException.class).rootCause().hasMessageContaining("row-level security");
    }

    @Test
    void givenAnAnonymousScope_whenWritingAnAnonymousAuditEvent_thenItIsAccepted() {
        int inserted = inScope(new AccessScope.Anonymous(), () -> jdbc.sql("""
                INSERT INTO audit_event (id, occurred_at, event_type, outcome, correlation_id)
                VALUES (gen_random_uuid(), now(), 'LOGIN', 'FAILURE', 'correlacao-anonima')
                """).update());

        assertThat(inserted).isEqualTo(1);
    }
}
