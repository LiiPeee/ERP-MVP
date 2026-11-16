package br.com.erpcore.bootstrap;

import static br.com.erpcore.bootstrap.support.HttpApi.loginJson;
import static br.com.erpcore.bootstrap.support.HttpApi.registrationJson;
import static br.com.erpcore.bootstrap.support.HttpApi.uniqueEmail;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.erpcore.bootstrap.support.ApplicationHttpTest;
import br.com.erpcore.bootstrap.support.HttpApi;
import br.com.erpcore.bootstrap.support.HttpApi.Reply;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.TestPropertySource;

/** Limites das rotas públicas. Cada teste começa com a aplicação nova, porque o limitador guarda estado na memória. */
@ApplicationHttpTest
@TestPropertySource(properties = {
        "erp.web.rate-limit.login-per-ip=3",
        "erp.web.rate-limit.login-per-email=2",
        "erp.web.rate-limit.register-per-ip=2",
        "erp.web.rate-limit.refresh-per-ip=2"})
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
class RateLimitIT {

    private static final String PASSWORD = "senha-sintetica-12345";

    @Value("${local.server.port}")
    private int port;

    private Reply login(HttpApi api, String email) {
        return api.post("/api/auth/login", loginJson(email, PASSWORD), Map.of());
    }

    private void assertLimited(Reply reply) {
        assertThat(reply.status()).isEqualTo(429);
        assertThat(reply.json().get("code").asString()).isEqualTo("TOO_MANY_REQUESTS");
        assertThat(Long.parseLong(reply.header("Retry-After"))).isBetween(1L, 3600L);
    }

    @Test
    void givenTooManyLoginsForTheSameEmail_whenTryingAgain_thenItIsLimitedButAnotherEmailStillWorks() {
        HttpApi api = new HttpApi(port);
        String email = uniqueEmail("alvo");

        assertThat(login(api, email).status()).isEqualTo(401);
        assertThat(login(api, email).status()).isEqualTo(401);
        assertLimited(login(api, email));

        assertThat(login(api, uniqueEmail("outro")).status()).isEqualTo(401);
    }

    @Test
    void givenTooManyLoginsFromTheSameAddress_whenTryingAgain_thenEvenNewEmailsAreLimited() {
        HttpApi api = new HttpApi(port);

        for (int attempt = 0; attempt < 3; attempt++) {
            assertThat(login(api, uniqueEmail("rajada")).status()).isEqualTo(401);
        }

        assertLimited(login(api, uniqueEmail("rajada")));
    }

    @Test
    void givenTooManyRegistrationsFromTheSameAddress_whenTryingAgain_thenItIsLimited() {
        HttpApi api = new HttpApi(port);

        assertThat(api.post("/api/auth/register", registrationJson(uniqueEmail("a"), PASSWORD), Map.of()).status()).isEqualTo(201);
        assertThat(api.post("/api/auth/register", registrationJson(uniqueEmail("b"), PASSWORD), Map.of()).status()).isEqualTo(201);

        assertLimited(api.post("/api/auth/register", registrationJson(uniqueEmail("c"), PASSWORD), Map.of()));
    }

    @Test
    void givenTooManyRefreshesFromTheSameAddress_whenTryingAgain_thenItIsLimited() {
        HttpApi api = new HttpApi(port);
        Map<String, String> withCsrf = Map.of("Cookie", "XSRF-TOKEN=abc", "X-XSRF-TOKEN", "abc");

        assertThat(api.post("/api/auth/refresh", null, withCsrf).status()).isEqualTo(401);
        assertThat(api.post("/api/auth/refresh", null, withCsrf).status()).isEqualTo(401);

        assertLimited(api.post("/api/auth/refresh", null, withCsrf));
    }

    @Test
    void givenTheLimitWasReached_whenCallingAnotherRoute_thenItIsNotAffected() {
        HttpApi api = new HttpApi(port);
        for (int attempt = 0; attempt < 4; attempt++) {
            login(api, uniqueEmail("rajada"));
        }

        assertThat(api.get("/actuator/health", Map.of()).status()).isEqualTo(200);
    }
}
