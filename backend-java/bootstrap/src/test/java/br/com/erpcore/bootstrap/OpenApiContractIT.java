package br.com.erpcore.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.erpcore.bootstrap.support.ApplicationHttpTest;
import br.com.erpcore.bootstrap.support.HttpApi;
import br.com.erpcore.bootstrap.support.HttpApi.Reply;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * O contrato da API. O arquivo {@code openapi/openapi.json} é a fonte do cliente do frontend: se a API mudar e o
 * arquivo não, este teste falha. Para atualizar: {@code ./mvnw verify -Dopenapi.update=true}.
 */
@ApplicationHttpTest
@TestPropertySource(properties = "springdoc.api-docs.enabled=true")
class OpenApiContractIT {

    private static final Path SNAPSHOT = Path.of("..", "..", "openapi", "openapi.json");
    private static final JsonMapper SORTED = JsonMapper.builder()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .build();

    @Value("${local.server.port}")
    private int port;

    private JsonNode spec() {
        Reply reply = new HttpApi(port).get("/v3/api-docs", Map.of());
        assertThat(reply.status()).isEqualTo(200);
        return reply.json();
    }

    @Test
    void givenTheRunningApplication_whenReadingItsContract_thenEveryOperationOfThePhasesIsDescribed() {
        JsonNode paths = spec().get("paths");

        assertThat(spec().get("openapi").asString()).startsWith("3.");
        assertThat(paths.get("/api/auth/register").has("post")).isTrue();
        assertThat(paths.get("/api/auth/login").has("post")).isTrue();
        assertThat(paths.get("/api/auth/refresh").has("post")).isTrue();
        assertThat(paths.get("/api/auth/logout").has("post")).isTrue();
        assertThat(paths.get("/api/me").has("get")).isTrue();
        assertThat(paths.get("/api/company-profile").has("get")).isTrue();
        assertThat(paths.get("/api/company-profile").has("put")).isTrue();
        assertThat(paths.get("/api/contacts").has("get")).isTrue();
        assertThat(paths.get("/api/contacts").has("post")).isTrue();
        assertThat(paths.get("/api/contacts/{contactId}/vehicles").has("get")).isTrue();
        assertThat(paths.get("/api/contacts/{contactId}/vehicles").has("post")).isTrue();
        assertThat(paths.get("/api/quotes").has("get")).isTrue();
        assertThat(paths.get("/api/quotes").has("post")).isTrue();
        assertThat(paths.get("/api/quotes/{quoteId}").has("get")).isTrue();
        assertThat(paths.get("/api/quotes/{quoteId}/items").has("put")).isTrue();
        assertThat(paths.get("/api/quotes/{quoteId}/confirm").has("post")).isTrue();
        assertThat(paths.get("/api/quotes/{quoteId}/resend").has("post")).isTrue();
        assertThat(paths.get("/api/quotes/{quoteId}/cancel").has("post")).isTrue();
        assertThat(paths.get("/api/public/quotes/{token}").has("get")).isTrue();
        assertThat(paths.get("/api/public/quotes/{token}/approve").has("post")).isTrue();
        assertThat(paths.get("/api/public/quotes/{token}/reject").has("post")).isTrue();
        assertThat(paths.size()).isEqualTo(17);
    }

    @Test
    void givenTheResponsesWithABody_whenReadingTheirMediaType_thenTheyAreDeclaredAsJsonSoTheGeneratedClientParsesThem() {
        JsonNode paths = spec().get("paths");

        for (String operation : new String[] {"/api/auth/register:post:201", "/api/auth/login:post:200",
                "/api/auth/refresh:post:200", "/api/me:get:200"}) {
            String[] parts = operation.split(":");
            JsonNode content = paths.get(parts[0]).get(parts[1]).get("responses").get(parts[2]).get("content");
            assertThat(content.has("application/json")).as("%s declara application/json", operation).isTrue();
            assertThat(content.has("*/*")).as("%s não declara */*", operation).isFalse();
        }
    }

    @Test
    void givenTheContract_whenLookingAtSecurity_thenOnlyTheOwnerRoutesDeclareTheBearerToken() {
        JsonNode paths = spec().get("paths");

        assertThat(paths.get("/api/me").get("get").get("security").toString()).contains("bearerAuth");
        assertThat(paths.get("/api/quotes").get("post").get("security").toString()).contains("bearerAuth");
        assertThat(paths.get("/api/contacts").get("get").get("security").toString()).contains("bearerAuth");
        assertThat(paths.get("/api/auth/login").get("post").has("security")).isFalse();
        assertThat(paths.get("/api/public/quotes/{token}").get("get").has("security")).isFalse();
        assertThat(paths.get("/api/public/quotes/{token}/approve").get("post").has("security")).isFalse();
        assertThat(spec().get("components").get("securitySchemes").get("bearerAuth").get("scheme").asString())
                .isEqualTo("bearer");
    }

    @Test
    void givenTheSessionSchema_whenReadingIt_thenTheRefreshTokenIsNotPartOfTheBody() {
        JsonNode session = spec().get("components").get("schemas").get("SessionResponse").get("properties");

        assertThat(session.has("accessToken")).isTrue();
        assertThat(session.has("expiresAt")).isTrue();
        assertThat(session.toString()).doesNotContain("refresh");
    }

    @Test
    void givenTheNewResponses_whenReadingTheirMediaType_thenTheyAreDeclaredAsJson() {
        JsonNode paths = spec().get("paths");

        for (String operation : new String[] {"/api/quotes:get:200", "/api/quotes:post:201",
                "/api/quotes/{quoteId}/confirm:post:200", "/api/public/quotes/{token}:get:200",
                "/api/public/quotes/{token}/approve:post:200", "/api/contacts:post:201"}) {
            String[] parts = operation.split(":");
            JsonNode content = paths.get(parts[0]).get(parts[1]).get("responses").get(parts[2]).get("content");
            assertThat(content.has("application/json")).as("%s declara application/json", operation).isTrue();
            assertThat(content.has("*/*")).as("%s não declara */*", operation).isFalse();
        }
    }

    @Test
    void givenThePublicQuoteSchema_whenReadingIt_thenItHasNoPersonalDataFields() {
        String schema = spec().get("components").get("schemas").get("PublicQuoteView").toString();

        assertThat(schema).doesNotContain("phone").doesNotContain("plate").doesNotContain("contactName")
                .doesNotContain("email");
    }

    @Test
    void givenTheContract_whenComparingWithTheVersionedSnapshot_thenTheyAreIdentical() throws IOException {
        String current = normalized(spec());

        if (Boolean.getBoolean("openapi.update")) {
            Files.createDirectories(SNAPSHOT.getParent());
            Files.writeString(SNAPSHOT, current, StandardCharsets.UTF_8);
        }

        assertThat(SNAPSHOT).as("openapi/openapi.json existe (gere com -Dopenapi.update=true)").exists();
        assertThat(Files.readString(SNAPSHOT, StandardCharsets.UTF_8).replace("\r\n", "\n"))
                .as("o contrato mudou: rode ./mvnw verify -Dopenapi.update=true e versione o arquivo")
                .isEqualTo(current);
    }

    /** Ordem estável das chaves, sem a porta aleatória do servidor e com quebras de linha iguais em qualquer sistema. */
    private static String normalized(JsonNode spec) {
        Map<?, ?> tree = SORTED.convertValue(spec, Map.class);
        Map<Object, Object> withoutServers = new java.util.TreeMap<>(tree.entrySet().stream()
                .filter(entry -> !"servers".equals(entry.getKey()))
                .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        return SORTED.writeValueAsString(withoutServers).replace("\r\n", "\n") + "\n";
    }
}
