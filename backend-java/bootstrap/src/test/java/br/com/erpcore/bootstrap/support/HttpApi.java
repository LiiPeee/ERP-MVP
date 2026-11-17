package br.com.erpcore.bootstrap.support;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Cliente HTTP de teste: fala com o servidor de verdade e deixa os cookies à mostra, sem gerenciá-los. */
public final class HttpApi {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
    private final String baseUrl;

    public HttpApi(int port) {
        this.baseUrl = "http://localhost:" + port;
    }

    public record Reply(int status, String body, Map<String, List<String>> headers) {

        public String header(String name) {
            return headers.getOrDefault(name.toLowerCase(), List.of()).stream().findFirst().orElse(null);
        }

        public List<String> setCookies() {
            return headers.getOrDefault("set-cookie", List.of());
        }

        /** A linha inteira do Set-Cookie do cookie com este nome, ou {@code null}. */
        public String setCookie(String name) {
            return setCookies().stream().filter(line -> line.startsWith(name + "=")).findFirst().orElse(null);
        }

        public String cookieValue(String name) {
            String line = setCookie(name);
            if (line == null) {
                return null;
            }
            String value = line.substring(name.length() + 1);
            int end = value.indexOf(';');
            return end < 0 ? value : value.substring(0, end);
        }

        public JsonNode json() {
            return JSON.readTree(body);
        }
    }

    public Reply get(String path, Map<String, String> headers) {
        return send("GET", path, null, headers);
    }

    public Reply post(String path, String jsonBody, Map<String, String> headers) {
        return send("POST", path, jsonBody == null ? "" : jsonBody, headers);
    }

    public Reply send(String method, String path, String body, Map<String, String> headers) {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(baseUrl + path));
        Map<String, String> all = new LinkedHashMap<>(headers);
        if (body != null && !body.isEmpty()) {
            all.putIfAbsent("Content-Type", "application/json");
        }
        all.forEach(request::header);
        request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        try {
            HttpResponse<String> response = client.send(request.build(), BodyHandlers.ofString());
            Map<String, List<String>> lowerCased = new LinkedHashMap<>();
            response.headers().map().forEach((name, values) -> lowerCased.put(name.toLowerCase(), values));
            return new Reply(response.statusCode(), response.body(), lowerCased);
        } catch (IOException failure) {
            throw new IllegalStateException(failure);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(interrupted);
        }
    }

    public static Map<String, String> bearer(String accessToken) {
        return Map.of("Authorization", "Bearer " + accessToken);
    }

    public static String registrationJson(String email, String password) {
        return """
                {"companyName":"Padaria Fictícia Ltda","ownerName":"Ana Fictícia","email":"%s","password":"%s"}
                """.formatted(email, password).strip();
    }

    public static String loginJson(String email, String password) {
        return "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password);
    }

    public static String uniqueEmail(String label) {
        return label + "-" + UUID.randomUUID() + "@exemplo.test";
    }
}
