package br.com.erpcore.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.erpcore.adapter.persistence.support.PostgresTestDatabase;
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
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ContextConfiguration;

/** A aplicação inteira sobe com o papel real da aplicação, migra com o papel migrador e atende /actuator/health. */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = ApplicationStartupIT.DatabaseInitializer.class)
class ApplicationStartupIT {

    private static final int HTTP_OK = 200;

    static final class DatabaseInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext context) {
            TestPropertyValues.of(PostgresTestDatabase.applicationRoleProperties().toArray(String[]::new))
                    .applyTo(context);
        }
    }

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private JdbcClient jdbcClient;

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
    void givenMigratedDatabase_whenTheApplicationRoleReadsWithoutAScope_thenRowLevelSecurityShowsNothing() {
        Integer companies = jdbcClient.sql("SELECT count(*) FROM company").query(Integer.class).single();

        assertThat(companies).isZero();
    }
}
