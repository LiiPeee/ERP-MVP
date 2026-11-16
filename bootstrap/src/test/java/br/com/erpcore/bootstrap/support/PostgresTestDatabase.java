package br.com.erpcore.bootstrap.support;

import java.nio.file.Path;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * PostgreSQL descartável criado com o mesmo script de papéis usado no Docker Compose.
 * Todas as credenciais são sintéticas e só existem dentro do contêiner de teste.
 */
public final class PostgresTestDatabase {

    public static final String APP_ROLE = "erp_app_test";
    public static final String MIGRATOR_ROLE = "erp_migrator_test";

    private static final DockerImageName IMAGE = DockerImageName.parse("postgres:18.7-alpine");
    private static final Path ROLES_SCRIPT = Path.of("..", "infra", "postgres", "init", "01-roles.sql");
    private static final String ROLES_SCRIPT_IN_CONTAINER = "/docker-entrypoint-initdb.d/01-roles.sql";

    private static final String DATABASE_NAME = "erp_test";
    private static final String SUPERUSER = "bootstrap_superuser_test";
    private static final String SUPERUSER_PASSWORD = "fake-superuser-password";
    private static final String APP_PASSWORD = "fake-app-password";
    private static final String MIGRATOR_PASSWORD = "fake-migrator-password";

    private PostgresTestDatabase() {
    }

    public static PostgreSQLContainer newContainer() {
        return new PostgreSQLContainer(IMAGE)
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
    }

    public static void registerProperties(PostgreSQLContainer container, DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", container::getJdbcUrl);
        registry.add("spring.datasource.username", () -> APP_ROLE);
        registry.add("spring.datasource.password", () -> APP_PASSWORD);
        registry.add("spring.flyway.user", () -> MIGRATOR_ROLE);
        registry.add("spring.flyway.password", () -> MIGRATOR_PASSWORD);
    }
}
