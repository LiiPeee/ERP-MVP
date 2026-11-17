package br.com.erpcore.adapter.persistence.support;

import java.nio.file.Path;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * Um PostgreSQL descartável por JVM, criado com o mesmo script de papéis do Docker Compose. Todas as
 * credenciais são sintéticas e só existem dentro do contêiner. Os testes não dependem de banco vazio:
 * cada um usa os próprios identificadores (ou limpa as tabelas, no caso dos contratos).
 */
public final class PostgresTestDatabase {

    public static final String APP_ROLE = "erp_app_test";
    public static final String MIGRATOR_ROLE = "erp_migrator_test";
    public static final String SUPERUSER = "bootstrap_superuser_test";

    private static final DockerImageName IMAGE = DockerImageName.parse("postgres:18.7-alpine");
    private static final Path ROLES_SCRIPT = Path.of("..", "..", "infra", "postgres", "init", "01-roles.sql");
    private static final String ROLES_SCRIPT_IN_CONTAINER = "/docker-entrypoint-initdb.d/01-roles.sql";

    private static final String DATABASE_NAME = "erp_test";
    private static final String SUPERUSER_PASSWORD = "fake-superuser-password";
    private static final String APP_PASSWORD = "fake-app-password";
    private static final String MIGRATOR_PASSWORD = "fake-migrator-password";

    private static final PostgreSQLContainer CONTAINER = startContainer();

    private PostgresTestDatabase() {
    }

    private static PostgreSQLContainer startContainer() {
        PostgreSQLContainer container = new PostgreSQLContainer(IMAGE)
                .withDatabaseName(DATABASE_NAME)
                .withUsername(SUPERUSER)
                .withPassword(SUPERUSER_PASSWORD)
                .withEnv("ERP_DB_MIGRATOR_USER", MIGRATOR_ROLE)
                .withEnv("ERP_DB_MIGRATOR_PASSWORD", MIGRATOR_PASSWORD)
                .withEnv("ERP_DB_APP_USER", APP_ROLE)
                .withEnv("ERP_DB_APP_PASSWORD", APP_PASSWORD)
                .withCopyFileToContainer(
                        MountableFile.forHostPath(ROLES_SCRIPT.toAbsolutePath().normalize()),
                        ROLES_SCRIPT_IN_CONTAINER);
        container.start();
        return container;
    }

    /** Propriedades do Spring para a aplicação: conecta como o papel da aplicação; migra como o papel migrador. */
    public static List<String> applicationRoleProperties() {
        return properties(APP_ROLE, APP_PASSWORD);
    }

    /** Propriedades do Spring que conectam como superusuário (ignora RLS): só para os testes de contrato. */
    public static List<String> superuserProperties() {
        return properties(SUPERUSER, SUPERUSER_PASSWORD);
    }

    private static List<String> properties(String user, String password) {
        return List.of(
                "spring.datasource.url=" + CONTAINER.getJdbcUrl(),
                "spring.datasource.username=" + user,
                "spring.datasource.password=" + password,
                "spring.flyway.user=" + MIGRATOR_ROLE,
                "spring.flyway.password=" + MIGRATOR_PASSWORD,
                "spring.flyway.placeholders.app_role=" + APP_ROLE);
    }

    public static JdbcClient superuserJdbc() {
        return JdbcClient.create(dataSource(SUPERUSER, SUPERUSER_PASSWORD));
    }

    public static JdbcClient migratorJdbc() {
        return JdbcClient.create(dataSource(MIGRATOR_ROLE, MIGRATOR_PASSWORD));
    }

    private static DataSource dataSource(String user, String password) {
        return new DriverManagerDataSource(CONTAINER.getJdbcUrl(), user, password);
    }

    /** Apaga todos os dados. Só os testes de contrato usam, porque dependem de começar sem nada. */
    public static void truncateAll() {
        superuserJdbc().sql("TRUNCATE audit_event, refresh_token, company_membership, \"user\", company CASCADE").update();
    }
}
