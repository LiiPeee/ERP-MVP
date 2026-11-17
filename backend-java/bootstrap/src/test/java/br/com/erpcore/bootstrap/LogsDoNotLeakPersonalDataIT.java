package br.com.erpcore.bootstrap;

import static br.com.erpcore.bootstrap.support.HttpApi.bearer;
import static br.com.erpcore.bootstrap.support.HttpApi.loginJson;
import static br.com.erpcore.bootstrap.support.HttpApi.uniqueEmail;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.erpcore.adapter.infra.logging.PersonalDataMasker;
import br.com.erpcore.bootstrap.support.ApplicationHttpTest;
import br.com.erpcore.bootstrap.support.HttpApi;
import br.com.erpcore.bootstrap.support.HttpApi.Reply;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.read.ListAppender;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/**
 * Dado pessoal em log. Duas camadas, cada uma provada sozinha: (1) o código da aplicação não coloca dado pessoal
 * nem segredo em log (os eventos são capturados ANTES do mascaramento); (2) se algo escapar, o mascaramento
 * central troca na saída JSON real.
 */
@ApplicationHttpTest
@ExtendWith(OutputCaptureExtension.class)
class LogsDoNotLeakPersonalDataIT {

    private static final String PASSWORD = "senha-sintetica-12345";
    private static final String CPF = "123.456.789-09";
    private static final String CNPJ = "12.345.678/0001-95";

    @Value("${local.server.port}")
    private int port;

    private Logger root;
    private Level previousLevel;
    private ListAppender<ILoggingEvent> rawEvents;

    @BeforeEach
    void captureEveryLogEventBeforeMasking() {
        root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        previousLevel = root.getLevel();
        root.setLevel(Level.DEBUG);
        rawEvents = new ListAppender<>();
        rawEvents.start();
        root.addAppender(rawEvents);
    }

    @AfterEach
    void stopCapturing() {
        root.detachAppender(rawEvents);
        root.setLevel(previousLevel);
    }

    private List<String> everyTextOfEveryEvent() {
        List<String> texts = new ArrayList<>();
        for (ILoggingEvent event : new ArrayList<>(rawEvents.list)) {
            texts.add(event.getFormattedMessage());
            if (event.getThrowableProxy() != null) {
                texts.add(ThrowableProxyUtil.asString(event.getThrowableProxy()));
            }
            texts.addAll(event.getMDCPropertyMap().values());
            if (event.getKeyValuePairs() != null) {
                event.getKeyValuePairs().forEach(pair -> texts.add(String.valueOf(pair.value)));
            }
        }
        return texts;
    }

    @Test
    void givenAWholeSessionWithErrorsAndHostileInput_whenReadingEveryLogEvent_thenNoPersonalDataOrSecretWasLogged() {
        HttpApi api = new HttpApi(port);
        String email = uniqueEmail("log");
        String registration = """
                {"companyName":"Padaria %s Ltda","ownerName":"Ana %s","email":"%s","password":"%s"}
                """.formatted(CNPJ, CPF, email, PASSWORD).strip();

        api.post("/api/auth/register", registration, Map.of());
        api.post("/api/auth/register", registration, Map.of());
        api.post("/api/auth/register", "{\"email\":\"" + email + "\",\"password\":\"curta\"}", Map.of());
        api.post("/api/auth/register", "{isto não é json " + email, Map.of());
        Reply login = api.post("/api/auth/login", loginJson(email, PASSWORD), Map.of());
        api.post("/api/auth/login", loginJson(email, "senha-errada-sintetica"), Map.of());
        api.post("/api/auth/login", loginJson(uniqueEmail("ninguem"), PASSWORD), Map.of());
        String accessToken = login.json().get("accessToken").asString();
        String refreshToken = login.cookieValue("refresh_token");
        String csrf = login.cookieValue("XSRF-TOKEN");
        api.get("/api/me", bearer(accessToken));
        api.get("/api/me", bearer(accessToken + "x"));
        api.get("/api/me", bearer("isso.nao.e-um-jwt"));
        Map<String, String> cookies = Map.of("Cookie", "refresh_token=" + refreshToken + "; XSRF-TOKEN=" + csrf, "X-XSRF-TOKEN", csrf);
        api.post("/api/auth/refresh", null, cookies);
        api.post("/api/auth/refresh", null, cookies);
        api.post("/api/auth/logout", null, cookies);
        api.get("/api/nao-existe?email=" + email + "&cpf=" + CPF, Map.of());
        api.send("DELETE", "/api/auth/login", null, Map.of());
        api.post("/api/auth/register", "{\"companyName\":\"" + "a".repeat(40_000) + "\"}", Map.of());

        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(everyTextOfEveryEvent())
                .anyMatch(text -> text.contains("path=/api/auth/register")));
        List<String> texts = everyTextOfEveryEvent();
        assertThat(texts).hasSizeGreaterThan(20);
        for (String text : texts) {
            assertThat(PersonalDataMasker.mask(text)).as("o mascarador não teria o que trocar em: %s", text).isEqualTo(text);
            for (String secret : List.of(email, PASSWORD, accessToken, refreshToken, csrf, CPF, CNPJ)) {
                assertThat(text).as("segredo ou dado pessoal em log").doesNotContain(secret);
            }
        }
    }

    @Test
    void givenTheDefaultConfiguration_whenCheckingTheLoggersThatPrintEntitiesAndParameters_thenTheyAreLockedAtWarn() {
        root.setLevel(Level.TRACE);

        for (String name : List.of("org.hibernate", "org.hibernate.SQL", "org.hibernate.orm.jdbc.bind",
                "org.hibernate.event.internal", "org.springframework.jdbc.core", "org.springframework.data.jpa",
                "org.springframework.security.web.FilterChainProxy", "org.springframework.web.servlet.DispatcherServlet",
                "org.apache.coyote.http11", "org.apache.catalina.core")) {
            Logger logger = (Logger) LoggerFactory.getLogger(name);
            assertThat(logger.isDebugEnabled()).as("DEBUG ligado em %s", name).isFalse();
        }
    }

    @Test
    void givenSensitiveTextLoggedByAnyCode_whenTheJsonLogIsWritten_thenTheRealOutputIsMasked(CapturedOutput output) {
        String jwt = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0ZSJ9.YXNzaW5hdHVyYS1zaW50ZXRpY2E";
        org.slf4j.Logger probe = LoggerFactory.getLogger("teste.sintetico");

        probe.info("contato ana@exemplo.test cpf 123.456.789-09 cnpj 12345678000195 jwt {}", jwt);
        probe.error("falha de biblioteca", new IllegalStateException("erro para outra.pessoa@exemplo.test"));

        String logged = output.getAll();
        assertThat(logged).contains("[email]").contains("[cpf]").contains("[cnpj]").contains("[token]");
        assertThat(logged).doesNotContain("ana@exemplo.test").doesNotContain("outra.pessoa@exemplo.test")
                .doesNotContain("123.456.789-09").doesNotContain("12345678000195").doesNotContain(jwt);
        assertThat(logged.lines().filter(line -> line.contains("teste.sintetico")))
                .allSatisfy(line -> assertThat(line).startsWith("{").contains("\"@timestamp\"").contains("\"level\""));
    }

    @Test
    void givenAnAuthenticatedRequest_whenReadingItsLogLine_thenItCarriesTheCorrelationIdAndTheCompanyId(CapturedOutput output) {
        HttpApi api = new HttpApi(port);
        String email = uniqueEmail("mdc");
        Reply registered = api.post("/api/auth/register", "{\"companyName\":\"Empresa Fictícia\",\"ownerName\":\"Dono Fictício\","
                + "\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}", Map.of());
        String companyId = registered.json().get("companyId").asString();
        String accessToken = api.post("/api/auth/login", loginJson(email, PASSWORD), Map.of()).json().get("accessToken").asString();

        api.get("/api/me", Map.of("Authorization", "Bearer " + accessToken, "X-Correlation-Id", "corr-teste-123"));

        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(output.getAll().lines()
                .filter(line -> line.contains("path=/api/me") && line.contains("status=200")))
                .anySatisfy(line -> assertThat(line).startsWith("{").contains("\"correlation_id\":\"corr-teste-123\"")
                        .contains("\"company_id\":\"" + companyId + "\"")));
    }

    @Test
    void givenAQuoteFlowWithAPhoneAPlateANameAndTheLink_whenReadingEveryLogEvent_thenNoneOfThemWasLogged() {
        HttpApi api = new HttpApi(port);
        String email = uniqueEmail("fluxo");
        api.post("/api/auth/register", """
                {"companyName":"Oficina Sintética","ownerName":"Dono Sintético","email":"%s","password":"%s"}
                """.formatted(email, PASSWORD).strip(), Map.of());
        String owner = api.post("/api/auth/login", loginJson(email, PASSWORD), Map.of()).json().get("accessToken").asString();
        String name = "Cliente Muito Sintético";
        String phoneDigits = "11955554444";
        String plate = "QWE1R23";
        String contactId = api.post("/api/contacts", """
                {"name":"%s","phone":"(11) 95555-4444","email":"cliente.sintetico@exemplo.test"}""".formatted(name),
                bearer(owner)).json().get("id").asString();
        String vehicleId = api.post("/api/contacts/" + contactId + "/vehicles", """
                {"plate":"%s","brand":"Fiat","model":"Uno","year":2014,"odometerKm":1000}""".formatted(plate),
                bearer(owner)).json().get("id").asString();
        String quoteId = api.post("/api/quotes", """
                {"contactId":"%s","vehicleId":"%s","items":[{"description":"Troca de óleo","kind":"LABOR","quantity":"1","unitPrice":"80.00"}]}
                """.formatted(contactId, vehicleId).strip(), bearer(owner)).json().get("id").asString();
        String url = api.post("/api/quotes/" + quoteId + "/confirm", "{}", bearer(owner)).json().get("publicUrl").asString();
        String linkToken = url.substring(url.lastIndexOf('/') + 1);
        api.get("/api/contacts?name=Muito", bearer(owner));
        api.get("/api/public/quotes/" + linkToken, Map.of());
        api.get("/o/" + linkToken, Map.of());
        api.post("/api/public/quotes/" + linkToken + "/approve", "{}", Map.of());
        api.post("/api/public/quotes/" + linkToken + "/reject", "{}", Map.of());
        api.get("/api/public/quotes/" + "Q".repeat(43), Map.of());
        api.post("/api/contacts", """
                {"name":"%s","phone":"(11) 95555-4444","email":null}""".formatted(name), bearer(owner));
        api.post("/api/contacts/" + contactId + "/vehicles", """
                {"plate":"%s","brand":"Fiat","model":"Uno","year":2014}""".formatted(plate), bearer(owner));

        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(everyTextOfEveryEvent())
                .anyMatch(text -> text.contains("path=/api/public/quotes")));
        for (String text : everyTextOfEveryEvent()) {
            assertThat(PersonalDataMasker.mask(text)).as("o mascarador não teria o que trocar em: %s", text).isEqualTo(text);
            for (String secret : List.of(name, phoneDigits, "95555-4444", plate, "cliente.sintetico", linkToken,
                    "Q".repeat(43), owner)) {
                assertThat(text).as("dado pessoal ou token em log").doesNotContain(secret);
            }
        }
    }
}
