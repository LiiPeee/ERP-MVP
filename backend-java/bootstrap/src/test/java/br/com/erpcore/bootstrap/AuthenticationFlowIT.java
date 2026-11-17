package br.com.erpcore.bootstrap;

import static br.com.erpcore.bootstrap.support.HttpApi.bearer;
import static br.com.erpcore.bootstrap.support.HttpApi.loginJson;
import static br.com.erpcore.bootstrap.support.HttpApi.registrationJson;
import static br.com.erpcore.bootstrap.support.HttpApi.uniqueEmail;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.erpcore.bootstrap.support.ApplicationHttpTest;
import br.com.erpcore.bootstrap.support.HttpApi;
import br.com.erpcore.bootstrap.support.HttpApi.Reply;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

/** Cadastrar, entrar, ver o contexto, renovar e sair, pelo HTTP de verdade. */
@ApplicationHttpTest
class AuthenticationFlowIT {

    private static final String PASSWORD = "senha-sintetica-12345";
    private static final String REFRESH_COOKIE = "refresh_token";
    private static final String CSRF_COOKIE = "XSRF-TOKEN";
    private static final String CSRF_HEADER = "X-XSRF-TOKEN";

    @Value("${local.server.port}")
    private int port;

    private HttpApi api;
    private String email;

    @BeforeEach
    void aFreshAccount() {
        api = new HttpApi(port);
        email = uniqueEmail("fluxo");
    }

    private Reply register() {
        return api.post("/api/auth/register", registrationJson(email, PASSWORD), Map.of());
    }

    private Reply login(String password) {
        return api.post("/api/auth/login", loginJson(email, password), Map.of());
    }

    private Map<String, String> cookies(Reply loginReply) {
        return Map.of("Cookie", REFRESH_COOKIE + "=" + loginReply.cookieValue(REFRESH_COOKIE) + "; " + CSRF_COOKIE + "="
                + loginReply.cookieValue(CSRF_COOKIE), CSRF_HEADER, loginReply.cookieValue(CSRF_COOKIE));
    }

    @Test
    void givenValidData_whenRegistering_thenTheCompanyAndTheOwnerAreCreated() {
        Reply reply = register();

        assertThat(reply.status()).isEqualTo(201);
        assertThat(reply.json().get("companyId").asString()).matches("[0-9a-f-]{36}");
        assertThat(reply.json().get("userId").asString()).matches("[0-9a-f-]{36}");
    }

    @Test
    void givenAnEmailAlreadyRegistered_whenRegisteringAgain_thenItIsAConflict() {
        register();

        Reply reply = register();

        assertThat(reply.status()).isEqualTo(409);
        assertThat(reply.json().get("code").asString()).isEqualTo("EMAIL_ALREADY_REGISTERED");
    }

    @Test
    void givenAShortPassword_whenRegistering_thenTheFieldAndTheCodeAreReported() {
        Reply reply = api.post("/api/auth/register", registrationJson(email, "curta"), Map.of());

        assertThat(reply.status()).isEqualTo(422);
        assertThat(reply.json().get("code").asString()).isEqualTo("TOO_SHORT");
        assertThat(reply.json().get("field").asString()).isEqualTo("password");
    }

    @Test
    void givenMalformedJson_whenRegistering_thenItIsABadRequestWithoutInternalDetails() {
        Reply reply = api.post("/api/auth/register", "{isto não é json", Map.of());

        assertThat(reply.status()).isEqualTo(400);
        assertThat(reply.json().get("code").asString()).isEqualTo("INVALID_REQUEST");
        assertThat(reply.body()).doesNotContain("Exception").doesNotContain("at br.com").doesNotContain("jackson");
    }

    @Test
    void givenCorrectCredentials_whenLoggingIn_thenTheAccessTokenIsInTheBodyAndTheRefreshTokenOnlyInAHardenedCookie() {
        register();

        Reply reply = login(PASSWORD);

        assertThat(reply.status()).isEqualTo(200);
        assertThat(reply.json().get("accessToken").asString()).startsWith("eyJ");
        assertThat(reply.json().get("expiresAt").asString()).isNotBlank();
        String refresh = reply.cookieValue(REFRESH_COOKIE);
        assertThat(refresh).isNotBlank();
        assertThat(reply.body()).doesNotContain(refresh);
        assertThat(reply.setCookie(REFRESH_COOKIE)).contains("HttpOnly").contains("Secure").contains("SameSite=Strict")
                .contains("Path=/api/auth").containsPattern("Max-Age=(60479\\d|604800)").doesNotContain("Domain");
        assertThat(reply.setCookies()).as("cookies enviados no login").anyMatch(cookie -> cookie.startsWith(CSRF_COOKIE + "="));
        assertThat(reply.setCookie(CSRF_COOKIE)).contains("SameSite=Strict").contains("Secure").doesNotContain("HttpOnly")
                .contains("Path=/;");
        assertThat(reply.header("Cache-Control")).contains("no-store");
    }

    @Test
    void givenAWrongPasswordAndAnUnknownEmail_whenLoggingIn_thenTheAnswersAreIndistinguishable() {
        register();

        Reply wrongPassword = login("senha-errada-sintetica");
        Reply unknownEmail = api.post("/api/auth/login", loginJson(uniqueEmail("ninguem"), PASSWORD), Map.of());

        assertThat(wrongPassword.status()).isEqualTo(401);
        assertThat(unknownEmail.status()).isEqualTo(401);
        assertThat(wrongPassword.json().get("code").asString()).isEqualTo("INVALID_CREDENTIALS");
        assertThat(withoutCorrelationId(wrongPassword.body())).isEqualTo(withoutCorrelationId(unknownEmail.body()));
        assertThat(wrongPassword.setCookies()).noneMatch(cookie -> cookie.startsWith(REFRESH_COOKIE + "=") && !cookie.contains("Max-Age=0"));
    }

    private static String withoutCorrelationId(String body) {
        return body.replaceAll("\"correlationId\":\"[^\"]*\"", "");
    }

    @Test
    void givenAValidAccessToken_whenReadingTheContext_thenTheUserTheCompanyAndTheRoleAreShown() {
        register();
        String accessToken = login(PASSWORD).json().get("accessToken").asString();

        Reply reply = api.get("/api/me", bearer(accessToken));

        assertThat(reply.status()).isEqualTo(200);
        assertThat(reply.json().get("email").asString()).isEqualTo(email);
        assertThat(reply.json().get("displayName").asString()).isEqualTo("Ana Fictícia");
        assertThat(reply.json().get("companyName").asString()).isEqualTo("Padaria Fictícia Ltda");
        assertThat(reply.json().get("role").asString()).isEqualTo("OWNER");
    }

    @Test
    void givenAnAuthenticatedRequestCarryingTheCsrfCookie_whenAnswering_thenTheCsrfCookieIsNotCleared() {
        register();
        Reply login = login(PASSWORD);
        String accessToken = login.json().get("accessToken").asString();

        Reply me = api.get("/api/me", Map.of("Authorization", "Bearer " + accessToken,
                "Cookie", CSRF_COOKIE + "=" + login.cookieValue(CSRF_COOKIE)));

        assertThat(me.status()).isEqualTo(200);
        assertThat(me.setCookies()).as("o navegador precisa continuar com o cookie para mandar o cabeçalho")
                .noneMatch(cookie -> cookie.startsWith(CSRF_COOKIE + "=;") || cookie.contains("Max-Age=0"));
    }

    @Test
    void givenNoAccessToken_whenReadingTheContext_thenItIsUnauthorizedWithAGenericAnswer() {
        Reply reply = api.get("/api/me", Map.of());

        assertThat(reply.status()).isEqualTo(401);
        assertThat(reply.json().get("code").asString()).isEqualTo("UNAUTHENTICATED");
        assertThat(reply.header("WWW-Authenticate")).isEqualTo("Bearer");
    }

    @Test
    void givenAMalformedAccessToken_whenReadingTheContext_thenTheAnswerDoesNotExplainWhy() {
        Reply reply = api.get("/api/me", bearer("isso.nao.e-um-jwt"));

        assertThat(reply.status()).isEqualTo(401);
        assertThat(reply.header("WWW-Authenticate")).isEqualTo("Bearer");
        assertThat(reply.body()).doesNotContain("Jwt").doesNotContain("decode").doesNotContain("Nimbus");
    }

    @Test
    void givenTheRefreshCookieButNoCsrfHeader_whenRefreshing_thenItIsForbidden() {
        register();
        Reply login = login(PASSWORD);

        Reply reply = api.post("/api/auth/refresh", null,
                Map.of("Cookie", REFRESH_COOKIE + "=" + login.cookieValue(REFRESH_COOKIE)));

        assertThat(reply.status()).isEqualTo(403);
    }

    @Test
    void givenAWrongCsrfHeader_whenRefreshing_thenItIsForbidden() {
        register();
        Reply login = login(PASSWORD);

        Reply reply = api.post("/api/auth/refresh", null, Map.of(
                "Cookie", REFRESH_COOKIE + "=" + login.cookieValue(REFRESH_COOKIE) + "; " + CSRF_COOKIE + "=" + login.cookieValue(CSRF_COOKIE),
                CSRF_HEADER, "valor-forjado"));

        assertThat(reply.status()).isEqualTo(403);
    }

    @Test
    void givenTheCookiesAndTheCsrfHeader_whenRefreshing_thenTheSessionIsRenewedWithANewCookie() {
        register();
        Reply login = login(PASSWORD);

        Reply reply = api.post("/api/auth/refresh", null, cookies(login));

        assertThat(reply.status()).isEqualTo(200);
        assertThat(reply.json().get("accessToken").asString()).startsWith("eyJ");
        assertThat(reply.cookieValue(REFRESH_COOKIE)).isNotBlank().isNotEqualTo(login.cookieValue(REFRESH_COOKIE));
        assertThat(api.get("/api/me", bearer(reply.json().get("accessToken").asString())).status()).isEqualTo(200);
    }

    @Test
    void givenARotatedRefreshToken_whenItIsUsedAgain_thenItIsRejectedTheCookieIsClearedAndTheSessionEnds() {
        register();
        Reply login = login(PASSWORD);
        Reply renewed = api.post("/api/auth/refresh", null, cookies(login));

        Reply reuse = api.post("/api/auth/refresh", null, cookies(login));

        assertThat(reuse.status()).isEqualTo(401);
        assertThat(reuse.json().get("code").asString()).isEqualTo("INVALID_REFRESH_TOKEN");
        assertThat(reuse.setCookie(REFRESH_COOKIE)).contains("Max-Age=0");
        Reply afterReuse = api.post("/api/auth/refresh", null, Map.of(
                "Cookie", REFRESH_COOKIE + "=" + renewed.cookieValue(REFRESH_COOKIE) + "; " + CSRF_COOKIE + "=" + login.cookieValue(CSRF_COOKIE),
                CSRF_HEADER, login.cookieValue(CSRF_COOKIE)));
        assertThat(afterReuse.status()).isEqualTo(401);
    }

    @Test
    void givenNoRefreshCookie_whenRefreshing_thenItIsUnauthorized() {
        register();
        Reply login = login(PASSWORD);

        Reply reply = api.post("/api/auth/refresh", null,
                Map.of("Cookie", CSRF_COOKIE + "=" + login.cookieValue(CSRF_COOKIE), CSRF_HEADER, login.cookieValue(CSRF_COOKIE)));

        assertThat(reply.status()).isEqualTo(401);
    }

    @Test
    void givenALoggedInUser_whenLoggingOut_thenTheCookieIsClearedAndTheTokenStopsWorking() {
        register();
        Reply login = login(PASSWORD);

        Reply logout = api.post("/api/auth/logout", null, cookies(login));

        assertThat(logout.status()).isEqualTo(204);
        assertThat(logout.setCookie(REFRESH_COOKIE)).contains("Max-Age=0").contains("HttpOnly").contains("Path=/api/auth");
        assertThat(api.post("/api/auth/refresh", null, cookies(login)).status()).isEqualTo(401);
    }

    @Test
    void givenNoCsrfHeader_whenLoggingOut_thenItIsForbiddenAndTheSessionSurvives() {
        register();
        Reply login = login(PASSWORD);

        Reply logout = api.post("/api/auth/logout", null,
                Map.of("Cookie", REFRESH_COOKIE + "=" + login.cookieValue(REFRESH_COOKIE)));

        assertThat(logout.status()).isEqualTo(403);
        assertThat(api.post("/api/auth/refresh", null, cookies(login)).status()).isEqualTo(200);
    }

    @Test
    void givenAnInvalidCorrelationIdFromTheClient_whenCalling_thenANewOneIsGeneratedAndReturned() {
        Reply hostile = api.get("/api/me", Map.of("X-Correlation-Id", "<script>alert(1)</script>"));
        Reply good = api.get("/api/me", Map.of("X-Correlation-Id", "abc-123"));

        assertThat(hostile.header("X-Correlation-Id")).matches("[0-9a-f-]{36}");
        assertThat(good.header("X-Correlation-Id")).isEqualTo("abc-123");
    }

    @Test
    void givenABodyBiggerThanTheLimit_whenRegistering_thenItIsRefusedBeforeBeingProcessed() {
        String huge = "{\"companyName\":\"" + "a".repeat(40_000) + "\"}";

        Reply reply = api.post("/api/auth/register", huge, Map.of());

        assertThat(reply.status()).isEqualTo(413);
    }
}
