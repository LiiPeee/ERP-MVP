package br.com.erpcore.adapter.persistence.security;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.erpcore.adapter.persistence.support.PostgresTestDatabase;
import br.com.erpcore.adapter.persistence.support.SuperuserPostgresTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Invariantes do catálogo do banco: valem para qualquer tabela futura, não só para as de hoje. */
@SuperuserPostgresTest
class DatabaseRolesIT {

    private static final String APP = PostgresTestDatabase.APP_ROLE;
    private static final String MIGRATIONS_HISTORY = "flyway_schema_history";

    private final JdbcClient database = PostgresTestDatabase.superuserJdbc();

    private List<String> businessTables() {
        return database.sql("SELECT tablename FROM pg_tables WHERE schemaname = 'public' AND tablename <> :history")
                .param("history", MIGRATIONS_HISTORY).query(String.class).list();
    }

    @Test
    void givenTheApplicationRole_whenInspectingIt_thenItHasNoElevatedPrivilege() {
        var role = database.sql("""
                SELECT rolsuper, rolbypassrls, rolcreaterole, rolcreatedb, rolreplication, rolcanlogin
                  FROM pg_roles WHERE rolname = :role
                """).param("role", APP).query().singleRow();

        assertThat(role).containsEntry("rolsuper", false).containsEntry("rolbypassrls", false)
                .containsEntry("rolcreaterole", false).containsEntry("rolcreatedb", false)
                .containsEntry("rolreplication", false).containsEntry("rolcanlogin", true);
    }

    @Test
    void givenTheApplicationRole_whenListingWhatItOwns_thenItOwnsNoTableAndNoFunction() {
        var ownedTables = database.sql("SELECT tablename FROM pg_tables WHERE tableowner = :role")
                .param("role", APP).query(String.class).list();
        var ownedFunctions = database.sql("SELECT proname FROM pg_proc WHERE proowner = (SELECT oid FROM pg_roles WHERE rolname = :role)")
                .param("role", APP).query(String.class).list();

        assertThat(ownedTables).isEmpty();
        assertThat(ownedFunctions).isEmpty();
    }

    @Test
    void givenEveryTable_whenCheckingRowLevelSecurity_thenItIsEnabledAndForced() {
        var unprotected = database.sql("""
                SELECT c.relname FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
                 WHERE n.nspname = 'public' AND c.relkind = 'r' AND c.relname <> :history
                   AND NOT (c.relrowsecurity AND c.relforcerowsecurity)
                """).param("history", MIGRATIONS_HISTORY).query(String.class).list();

        assertThat(businessTables()).isNotEmpty();
        assertThat(unprotected).isEmpty();
    }

    @Test
    void givenAnyTableWithACompanyColumn_whenCheckingIt_thenRowLevelSecurityIsForced() {
        var tenantTablesWithoutRls = database.sql("""
                SELECT col.table_name FROM information_schema.columns col
                  JOIN pg_class c ON c.relname = col.table_name
                 WHERE col.table_schema = 'public' AND col.column_name = 'company_id'
                   AND NOT (c.relrowsecurity AND c.relforcerowsecurity)
                """).query(String.class).list();

        assertThat(tenantTablesWithoutRls).isEmpty();
    }

    @Test
    void givenEveryPolicy_whenReadingItsExpressions_thenItIsGuardedByTheAccessScope() {
        var unguarded = database.sql("""
                SELECT polname FROM pg_policy
                 WHERE coalesce(pg_get_expr(polqual, polrelid), '') NOT LIKE '%app_scope()%'
                   AND coalesce(pg_get_expr(polwithcheck, polrelid), '') NOT LIKE '%app_scope()%'
                """).query(String.class).list();

        assertThat(unguarded).isEmpty();
    }

    @Test
    void givenTheApplicationRole_whenCheckingPrivileges_thenItCanNeverDeleteAndNeverTouchTheMigrationHistory() {
        for (String table : businessTables()) {
            assertThat(hasPrivilege(table, "DELETE")).as("DELETE on %s", table).isFalse();
            assertThat(hasPrivilege(table, "TRUNCATE")).as("TRUNCATE on %s", table).isFalse();
            assertThat(hasPrivilege(table, "REFERENCES")).as("REFERENCES on %s", table).isFalse();
        }
        assertThat(hasPrivilege(MIGRATIONS_HISTORY, "SELECT")).isFalse();
        assertThat(hasPrivilege(MIGRATIONS_HISTORY, "INSERT")).isFalse();
    }

    @Test
    void givenTheAuditTrail_whenCheckingPrivileges_thenTheApplicationCanOnlyInsert() {
        assertThat(hasPrivilege("audit_event", "INSERT")).isTrue();
        assertThat(hasPrivilege("audit_event", "SELECT")).isFalse();
        assertThat(hasPrivilege("audit_event", "UPDATE")).isFalse();
    }

    @Test
    void givenImmutableColumns_whenCheckingPrivileges_thenTheApplicationCannotUpdateThem() {
        assertThat(hasColumnPrivilege("user", "email", "UPDATE")).isFalse();
        assertThat(hasColumnPrivilege("user", "password_hash", "UPDATE")).isFalse();
        assertThat(hasColumnPrivilege("user", "failed_login_count", "UPDATE")).isTrue();
        assertThat(hasColumnPrivilege("refresh_token", "expires_at", "UPDATE")).isFalse();
        assertThat(hasColumnPrivilege("refresh_token", "revoked_at", "UPDATE")).isTrue();
    }

    @Test
    void givenThePublicSchema_whenCheckingCreationRights_thenTheApplicationCannotCreateObjects() {
        var canCreate = database.sql("SELECT has_schema_privilege(:role, 'public', 'CREATE')")
                .param("role", APP).query(Boolean.class).single();

        assertThat(canCreate).isFalse();
    }

    private boolean hasPrivilege(String table, String privilege) {
        return database.sql("SELECT has_table_privilege(:role, format('public.%I', :table::text), :privilege)")
                .param("role", APP).param("table", table).param("privilege", privilege)
                .query(Boolean.class).single();
    }

    private boolean hasColumnPrivilege(String table, String column, String privilege) {
        return database.sql("SELECT has_column_privilege(:role, format('public.%I', :table::text), :column, :privilege)")
                .param("role", APP).param("table", table).param("column", column).param("privilege", privilege)
                .query(Boolean.class).single();
    }
}
