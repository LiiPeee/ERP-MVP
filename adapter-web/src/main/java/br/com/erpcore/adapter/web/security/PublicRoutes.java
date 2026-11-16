package br.com.erpcore.adapter.web.security;

import java.util.List;
import org.springframework.http.HttpMethod;

/**
 * A lista completa e explícita do que pode ser chamado sem token de acesso. Tudo o mais exige autenticação, e o
 * que nem existe é negado. O teste de rotas confere esta lista contra as rotas reais da aplicação.
 */
public final class PublicRoutes {

    public record Route(HttpMethod method, String pattern) {
    }

    public static final List<Route> ALL = List.of(
            new Route(HttpMethod.POST, "/api/auth/register"),
            new Route(HttpMethod.POST, "/api/auth/login"),
            new Route(HttpMethod.POST, "/api/auth/refresh"),
            new Route(HttpMethod.POST, "/api/auth/logout"),
            new Route(HttpMethod.GET, "/actuator/health"),
            new Route(HttpMethod.GET, "/actuator/health/**"));

    private PublicRoutes() {
    }
}
