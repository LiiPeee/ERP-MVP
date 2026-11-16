package br.com.erpcore.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.erpcore.adapter.persistence.support.PostgresTestDatabase;
import br.com.erpcore.adapter.persistence.support.SuperuserPostgresTest;
import java.util.List;
import org.junit.jupiter.api.Test;

/** O contexto sobe e aplica as migrações do zero sobre um banco recém-criado (o PostgreSQL do contêiner). */
@SuperuserPostgresTest
class MigrationsFromScratchIT {

    private static final List<String> EXPECTED_TABLES =
            List.of("audit_event", "company", "company_membership", "refresh_token", "user");

    @Test
    void givenAnEmptyDatabase_whenTheApplicationStarts_thenEveryMigrationIsAppliedSuccessfully() {
        var failed = PostgresTestDatabase.superuserJdbc()
                .sql("SELECT version FROM flyway_schema_history WHERE NOT success")
                .query(String.class).list();
        var applied = PostgresTestDatabase.superuserJdbc()
                .sql("SELECT count(*) FROM flyway_schema_history WHERE success AND version IS NOT NULL")
                .query(Integer.class).single();

        assertThat(failed).isEmpty();
        assertThat(applied).isPositive();
    }

    @Test
    void givenTheMigratedDatabase_whenListingTables_thenTheFivePhaseZeroTablesExist() {
        var tables = PostgresTestDatabase.superuserJdbc()
                .sql("SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' "
                        + "AND table_name <> 'flyway_schema_history' ORDER BY table_name")
                .query(String.class).list();

        assertThat(tables).containsExactlyElementsOf(EXPECTED_TABLES);
    }

    @Test
    void givenTheMigratedDatabase_whenListingTheirOwners_thenTheMigratorRoleOwnsEveryTable() {
        var owners = PostgresTestDatabase.superuserJdbc()
                .sql("SELECT DISTINCT tableowner FROM pg_tables WHERE schemaname = 'public'")
                .query(String.class).list();

        assertThat(owners).containsExactly(PostgresTestDatabase.MIGRATOR_ROLE);
    }

    @Test
    void givenTheAuditTrail_whenCheckingConstraints_thenItHasNoForeignKeysSoItNeverBlocksAWriteOrADeletion() {
        var foreignKeys = PostgresTestDatabase.superuserJdbc().sql("""
                SELECT conname FROM pg_constraint
                 WHERE conrelid = 'public.audit_event'::regclass AND contype = 'f'
                """).query(String.class).list();

        assertThat(foreignKeys).isEmpty();
    }

    @Test
    void givenTheMigratedDatabase_whenCheckingPrimaryKeys_thenEveryTableUsesAUuid() {
        var nonUuidKeys = PostgresTestDatabase.superuserJdbc().sql("""
                SELECT c.table_name
                  FROM information_schema.columns c
                 WHERE c.table_schema = 'public' AND c.column_name = 'id' AND c.data_type <> 'uuid'
                   AND c.table_name <> 'flyway_schema_history'
                """).query(String.class).list();

        assertThat(nonUuidKeys).isEmpty();
    }
}
