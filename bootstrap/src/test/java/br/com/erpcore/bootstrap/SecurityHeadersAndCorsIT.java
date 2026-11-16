package br.com.erpcore.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.erpcore.bootstrap.support.ApplicationHttpTest;
import br.com.erpcore.bootstrap.support.HttpApi;
import br.com.erpcore.bootstrap.support.HttpApi.Reply;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

/** Cabeçalhos de segurança, CSP sem recursos de terceiros e CORS fechado (nenhuma origem externa liberada). */
@ApplicationHttpTest
class SecurityHeadersAndCorsIT {

    private static final String EVIL_ORIGIN = "https://site-malicioso.example";

    @Value("${local.server.port}")
    private int port;

    private HttpApi api() {
        return new HttpApi(port);
    }

    @Test
    void givenAnyResponse_whenReadingItsHeaders_thenTheRestrictiveSecurityHeadersAreThere() {
        for (Reply reply : new Reply[] {api().get("/api/me", Map.of()), api().get("/actuator/health", Map.of())}) {
            assertThat(reply.header("X-Content-Type-Options")).isEqualTo("nosniff");
            assertThat(reply.header("X-Frame-Options")).isEqualTo("DENY");
            assertThat(reply.header("Referrer-Policy")).isEqualTo("no-referrer");
            assertThat(reply.header("Cross-Origin-Opener-Policy")).isEqualTo("same-origin");
            assertThat(reply.header("Cross-Origin-Resource-Policy")).isEqualTo("same-origin");
            assertThat(reply.header("Permissions-Policy")).contains("camera=()").contains("microphone=()").contains("geolocation=()");
            assertThat(reply.header("Cache-Control")).contains("no-store");
        }
    }

    @Test
    void givenTheContentSecurityPolicy_whenReadingIt_thenItAllowsOnlyTheOwnOriginAndNoThirdParty() {
        String csp = api().get("/api/me", Map.of()).header("Content-Security-Policy");

        assertThat(csp).contains("default-src 'self'").contains("script-src 'self'").contains("object-src 'none'")
                .contains("frame-ancestors 'none'").contains("base-uri 'self'").contains("form-action 'self'");
        assertThat(csp).doesNotContain("http:").doesNotContain("https:").doesNotContain("*")
                .doesNotContain("unsafe-eval").doesNotContain("unsafe-inline");
    }

    @Test
    void givenAnyResponse_whenReadingItsHeaders_thenNoServerOrFrameworkVersionIsRevealed() {
        Reply reply = api().get("/actuator/health", Map.of());

        assertThat(reply.header("Server")).isNull();
        assertThat(reply.header("X-Powered-By")).isNull();
    }

    @Test
    void givenAPreflightFromAForeignOrigin_whenAsking_thenNoCorsPermissionIsGranted() {
        Reply reply = api().send("OPTIONS", "/api/me", null, Map.of("Origin", EVIL_ORIGIN,
                "Access-Control-Request-Method", "GET", "Access-Control-Request-Headers", "authorization"));

        assertThat(reply.header("Access-Control-Allow-Origin")).isNull();
        assertThat(reply.header("Access-Control-Allow-Credentials")).isNull();
        assertThat(reply.status()).isNotBetween(200, 299);
    }

    @Test
    void givenARequestFromAForeignOrigin_whenAnswering_thenNoCorsPermissionIsGranted() {
        Reply reply = api().get("/actuator/health", Map.of("Origin", EVIL_ORIGIN));

        assertThat(reply.header("Access-Control-Allow-Origin")).isNull();
    }

    @Test
    void givenAnErrorResponse_whenReadingTheBody_thenThereIsNoStackTraceOrInternalName() {
        Reply notFound = api().get("/api/nao-existe", Map.of());
        Reply wrongMethod = api().send("DELETE", "/api/auth/login", null, Map.of());
        Reply badJson = api().post("/api/auth/login", "{", Map.of());

        for (Reply reply : new Reply[] {notFound, wrongMethod, badJson}) {
            assertThat(reply.body()).doesNotContain("Exception").doesNotContain("\tat ").doesNotContain("org.springframework")
                    .doesNotContain("br.com.erpcore").doesNotContain("trace");
        }
    }
}
