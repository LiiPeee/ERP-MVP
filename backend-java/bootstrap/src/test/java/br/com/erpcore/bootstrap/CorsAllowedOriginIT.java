package br.com.erpcore.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.erpcore.bootstrap.support.ApplicationHttpTest;
import br.com.erpcore.bootstrap.support.HttpApi;
import br.com.erpcore.bootstrap.support.HttpApi.Reply;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;

/** Com uma origem liberada de propósito (o servidor de desenvolvimento do Angular), só ela é aceita. */
@ApplicationHttpTest
@TestPropertySource(properties = "erp.web.allowed-origins=http://localhost:4200")
class CorsAllowedOriginIT {

    @Value("${local.server.port}")
    private int port;

    private Reply preflight(String origin) {
        return new HttpApi(port).send("OPTIONS", "/api/auth/login", null, Map.of("Origin", origin,
                "Access-Control-Request-Method", "POST", "Access-Control-Request-Headers", "content-type"));
    }

    @Test
    void givenTheConfiguredOrigin_whenPreflighting_thenItIsAllowedWithCredentials() {
        Reply reply = preflight("http://localhost:4200");

        assertThat(reply.status()).isEqualTo(200);
        assertThat(reply.header("Access-Control-Allow-Origin")).isEqualTo("http://localhost:4200");
        assertThat(reply.header("Access-Control-Allow-Credentials")).isEqualTo("true");
        assertThat(reply.header("Access-Control-Allow-Methods")).contains("POST").doesNotContain("DELETE");
    }

    @Test
    void givenAnotherOrigin_whenPreflighting_thenItIsRefused() {
        Reply reply = preflight("http://localhost:4201");

        assertThat(reply.header("Access-Control-Allow-Origin")).isNull();
        assertThat(reply.status()).isEqualTo(403);
    }

    @Test
    void givenAWildcardLookalike_whenPreflighting_thenItIsRefused() {
        assertThat(preflight("http://localhost:4200.site-malicioso.example").header("Access-Control-Allow-Origin")).isNull();
        assertThat(preflight("null").header("Access-Control-Allow-Origin")).isNull();
    }

    @Test
    void givenThePutMethod_whenPreflightingAnOwnerRoute_thenItIsAllowedButDeleteStillIsNot() {
        Reply reply = new HttpApi(port).send("OPTIONS", "/api/company-profile", null, Map.of("Origin", "http://localhost:4200",
                "Access-Control-Request-Method", "PUT", "Access-Control-Request-Headers", "authorization,content-type"));

        assertThat(reply.status()).isEqualTo(200);
        assertThat(reply.header("Access-Control-Allow-Methods")).contains("PUT").doesNotContain("DELETE");
    }
}
