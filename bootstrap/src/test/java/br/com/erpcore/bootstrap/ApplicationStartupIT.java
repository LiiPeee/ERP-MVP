package br.com.erpcore.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.erpcore.bootstrap.support.PostgresTestDatabase;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class ApplicationStartupIT {

    private static final int HTTP_OK = 200;

    @Container
    private static final PostgreSQLContainer POSTGRES = PostgresTestDatabase.newContainer();

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private JdbcClient jdbcClient;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.registerProperties(POSTGRES, registry);
    }

    @Test
    void givenRunningDatabase_whenApplicationStarts_thenHealthEndpointReportsUp() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/actuator/health"))
                .GET()
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(HTTP_OK);
        assertThat(response.body()).contains("\"status\":\"UP\"");
    }

    @Test
    void givenApplicationDatasource_whenInspectingItsRole_thenItHasNoElevatedPrivileges() {
        Map<String, Object> role = jdbcClient.sql("""
                        SELECT r.rolname            AS role_name,
                               r.rolsuper           AS is_superuser,
                               r.rolbypassrls       AS bypasses_row_security,
                               r.rolcreaterole      AS can_create_roles,
                               r.rolcreatedb        AS can_create_databases,
                               d.datdba = r.oid     AS owns_database
                          FROM pg_roles r
                          JOIN pg_database d ON d.datname = current_database()
                         WHERE r.rolname = current_user
                        """)
                .query()
                .singleRow();

        assertThat(role)
                .containsEntry("role_name", PostgresTestDatabase.APP_ROLE)
                .containsEntry("is_superuser", false)
                .containsEntry("bypasses_row_security", false)
                .containsEntry("can_create_roles", false)
                .containsEntry("can_create_databases", false)
                .containsEntry("owns_database", false);
    }

    @Test
    void givenMigratorRole_whenInspectingTheDatabase_thenItOwnsTheDatabaseInsteadOfTheApplicationRole() {
        String databaseOwner = jdbcClient.sql("""
                        SELECT pg_get_userbyid(datdba)
                          FROM pg_database
                         WHERE datname = current_database()
                        """)
                .query(String.class)
                .single();

        assertThat(databaseOwner).isEqualTo(PostgresTestDatabase.MIGRATOR_ROLE);
    }
}
