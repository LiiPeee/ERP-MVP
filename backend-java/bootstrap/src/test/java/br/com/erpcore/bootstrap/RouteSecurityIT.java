package br.com.erpcore.bootstrap;

import static br.com.erpcore.bootstrap.support.HttpApi.bearer;
import static br.com.erpcore.bootstrap.support.HttpApi.loginJson;
import static br.com.erpcore.bootstrap.support.HttpApi.registrationJson;
import static br.com.erpcore.bootstrap.support.HttpApi.uniqueEmail;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.erpcore.adapter.web.security.PublicRoutes;
import br.com.erpcore.bootstrap.support.ApplicationHttpTest;
import br.com.erpcore.bootstrap.support.HttpApi;
import br.com.erpcore.bootstrap.support.HttpApi.Reply;
import br.com.erpcore.bootstrap.support.TestTokens;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;

/**
 * Percorre TODAS as rotas que a aplicação realmente registra (controllers, actuator, erro) e garante que nenhuma
 * ficou sem autenticação nem sem verificação de empresa: só as rotas da lista explícita de públicas passam sem
 * token; todas as outras recusam anônimos, tokens forjados, vencidos, sem assinatura e de empresa alheia.
 */
@ApplicationHttpTest
class RouteSecurityIT {

    private static final String PASSWORD = "senha-sintetica-12345";
    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private ApplicationContext context;

    private record Route(String method, String path) {
        @Override
        public String toString() {
            return method + " " + path;
        }
    }

    private record Account(UUID companyId, UUID userId, String email) {
    }

    private List<Route> allRegisteredRoutes() {
        Set<Route> routes = new TreeSet<>(java.util.Comparator.comparing(Route::toString));
        for (RequestMappingInfoHandlerMapping mapping : context.getBeansOfType(RequestMappingInfoHandlerMapping.class).values()) {
            for (RequestMappingInfo info : mapping.getHandlerMethods().keySet()) {
                Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
                for (String pattern : info.getPatternValues()) {
                    String concrete = pattern.replaceAll("\\{[^}]*}", "x");
                    if (methods.isEmpty()) {
                        routes.add(new Route("GET", concrete));
                    }
                    methods.forEach(method -> routes.add(new Route(method.name(), concrete)));
                }
            }
        }
        return new ArrayList<>(routes);
    }

    private static boolean isDeclaredPublic(Route route) {
        return PublicRoutes.ALL.stream().anyMatch(declared -> declared.method().name().equals(route.method())
                && MATCHER.match(declared.pattern(), route.path()));
    }

    private Reply call(Route route, Map<String, String> headers) {
        return new HttpApi(port).send(route.method(), route.path(), route.method().equals("GET") ? null : "{}", headers);
    }

    private Account registerAccount(String label) {
        HttpApi api = new HttpApi(port);
        String email = uniqueEmail(label);
        Reply reply = api.post("/api/auth/register", registrationJson(email, PASSWORD), Map.of());
        assertThat(reply.status()).isEqualTo(201);
        return new Account(UUID.fromString(reply.json().get("companyId").asString()),
                UUID.fromString(reply.json().get("userId").asString()), email);
    }

    @Test
    void givenTheRealRoutes_whenListingThem_thenTheSensitiveOnesAreFoundAndNothingIsEmpty() {
        List<String> routes = allRegisteredRoutes().stream().map(Route::toString).toList();

        assertThat(routes).contains("POST /api/auth/register", "POST /api/auth/login", "POST /api/auth/refresh",
                "POST /api/auth/logout", "GET /api/me", "GET /actuator/health");
    }

    @Test
    void givenTheDeclaredPublicRoutes_whenComparingWithTheRealOnes_thenNoDeclarationIsStale() {
        List<Route> real = allRegisteredRoutes();

        for (PublicRoutes.Route declared : PublicRoutes.ALL) {
            assertThat(real).as("a rota pública declarada %s %s existe", declared.method(), declared.pattern())
                    .anyMatch(route -> route.method().equals(declared.method().name())
                            && MATCHER.match(declared.pattern(), route.path()));
        }
    }

    @Test
    void givenEveryNonPublicRoute_whenCalledWithoutCredentials_thenItIsUnauthorized() {
        List<Route> privateRoutes = allRegisteredRoutes().stream().filter(route -> !isDeclaredPublic(route)).toList();

        assertThat(privateRoutes).isNotEmpty();
        for (Route route : privateRoutes) {
            Reply reply = call(route, Map.of());
            assertThat(reply.status()).as("%s sem token", route).isEqualTo(401);
            assertThat(reply.header("WWW-Authenticate")).as("%s sem token", route).isEqualTo("Bearer");
        }
    }

    @Test
    void givenEveryPrivateApiRoute_whenCalledWithAForgedExpiredOrUnsignedToken_thenItIsUnauthorized() {
        Account account = registerAccount("rotas");
        List<Route> privateApiRoutes = allRegisteredRoutes().stream()
                .filter(route -> !isDeclaredPublic(route) && route.path().startsWith("/api/")).toList();
        Map<String, String> badTokens = Map.of(
                "lixo", "isto.nao.e-um-jwt",
                "outra chave", TestTokens.signedWithAnotherKey(account.userId(), account.companyId()),
                "vencido", TestTokens.expired(account.userId(), account.companyId()),
                "sem assinatura (alg none)", TestTokens.unsigned(account.userId(), account.companyId()),
                "sem empresa", TestTokens.withoutCompany(account.userId()),
                "chave desconhecida", TestTokens.withUnknownKeyId(account.userId(), account.companyId()));

        assertThat(privateApiRoutes).isNotEmpty();
        for (Route route : privateApiRoutes) {
            badTokens.forEach((description, token) ->
                    assertThat(call(route, bearer(token)).status()).as("%s com token %s", route, description).isEqualTo(401));
        }
    }

    @Test
    void givenEveryPrivateApiRoute_whenCalledWithAValidTokenOfAnotherCompany_thenItIsForbidden() {
        Account ana = registerAccount("ana");
        Account beto = registerAccount("beto");
        String tokenOfAnaClaimingBetosCompany = TestTokens.valid(ana.userId(), beto.companyId());
        List<Route> privateApiRoutes = allRegisteredRoutes().stream()
                .filter(route -> !isDeclaredPublic(route) && route.path().startsWith("/api/")).toList();

        for (Route route : privateApiRoutes) {
            Reply reply = call(route, bearer(tokenOfAnaClaimingBetosCompany));
            assertThat(reply.status()).as("%s com a empresa de outro", route).isEqualTo(403);
            assertThat(reply.body()).doesNotContain(beto.email()).doesNotContain("Padaria");
        }
    }

    @Test
    void givenAValidTokenOfAUnknownUser_whenReadingTheContext_thenItIsForbidden() {
        Reply reply = new HttpApi(port).get("/api/me", bearer(TestTokens.valid(UUID.randomUUID(), UUID.randomUUID())));

        assertThat(reply.status()).isEqualTo(403);
    }

    @Test
    void givenParametersAndHeadersNamingAnotherCompany_whenReadingTheContext_thenTheTenantStillComesOnlyFromTheToken() {
        Account ana = registerAccount("ana");
        Account beto = registerAccount("beto");
        HttpApi api = new HttpApi(port);
        String anaToken = api.post("/api/auth/login", loginJson(ana.email(), PASSWORD), Map.of())
                .json().get("accessToken").asString();

        Reply reply = api.get("/api/me?companyId=" + beto.companyId() + "&company_id=" + beto.companyId(), Map.of(
                "Authorization", "Bearer " + anaToken,
                "X-Company-Id", beto.companyId().toString(),
                "X-Tenant-Id", beto.companyId().toString()));

        assertThat(reply.status()).isEqualTo(200);
        assertThat(reply.json().get("companyId").asString()).isEqualTo(ana.companyId().toString());
        assertThat(reply.json().get("email").asString()).isEqualTo(ana.email());
    }

    @Test
    void givenTheFrontendRoutes_whenCalledAnonymously_thenOnlyReadingTheDeclaredStaticPathsIsAllowed() {
        HttpApi api = new HttpApi(port);

        // Sem os arquivos do frontend (nos testes) a resposta é 404: o que importa é que NÃO é 401.
        for (PublicRoutes.Route route : PublicRoutes.FRONTEND) {
            String path = route.pattern().replace("*", "app");
            assertThat(api.get(path, Map.of()).status()).as("GET %s", path).isNotEqualTo(401);
            if (!path.equals("/")) {
                assertThat(api.post(path, "{}", Map.of()).status()).as("POST %s", path).isEqualTo(401);
            }
        }
        assertThat(api.get("/assets/segredo.txt", Map.of()).status()).isEqualTo(401);
        assertThat(api.get("/app.map", Map.of()).status()).isEqualTo(401);
    }

    @Test
    void givenRoutesThatDoNotExist_whenCalled_thenAnonymousGetsUnauthorizedAndAnAuthenticatedUserGetsNothingInternal() {
        Account ana = registerAccount("ana");
        HttpApi api = new HttpApi(port);
        String token = api.post("/api/auth/login", loginJson(ana.email(), PASSWORD), Map.of())
                .json().get("accessToken").asString();

        for (String path : List.of("/admin", "/actuator/env", "/actuator/beans", "/actuator/heapdump", "/v3/api-docs",
                "/swagger-ui/index.html", "/h2-console", "/.env", "/api/does-not-exist")) {
            assertThat(api.get(path, Map.of()).status()).as("anônimo em %s", path).isEqualTo(401);
        }
        for (String path : List.of("/admin", "/actuator/env", "/actuator/beans", "/v3/api-docs")) {
            Reply reply = api.get(path, bearer(token));
            assertThat(reply.status()).as("autenticado em %s", path).isEqualTo(403);
            assertThat(reply.body()).doesNotContain("Exception").doesNotContain("springframework");
        }
    }
}
