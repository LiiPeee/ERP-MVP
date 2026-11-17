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
            // O link do orçamento: o token no caminho é a credencial. Ler (GET) nunca consome; só a decisão (POST).
            new Route(HttpMethod.GET, "/api/public/quotes/*"),
            new Route(HttpMethod.POST, "/api/public/quotes/*/approve"),
            new Route(HttpMethod.POST, "/api/public/quotes/*/reject"),
            new Route(HttpMethod.GET, "/actuator/health"),
            new Route(HttpMethod.GET, "/actuator/health/**"));

    /**
     * O frontend (Angular) servido pela própria aplicação: só leitura (GET) de arquivos estáticos e das rotas da
     * tela. O conteúdo do usuário continua atrás de /api/**, que exige token.
     */
    public static final List<Route> FRONTEND = List.of(
            new Route(HttpMethod.GET, "/"),
            new Route(HttpMethod.GET, "/index.html"),
            new Route(HttpMethod.GET, "/login"),
            new Route(HttpMethod.GET, "/register"),
            new Route(HttpMethod.GET, "/me"),
            // A página que o cliente abre pelo link do orçamento.
            new Route(HttpMethod.GET, "/o/*"),
            new Route(HttpMethod.GET, "/*.js"),
            new Route(HttpMethod.GET, "/*.css"),
            new Route(HttpMethod.GET, "/*.ico"),
            // Fontes locais (OFL): só .woff2 diretamente em /fonts. Os arquivos de licença da mesma pasta seguem negados.
            new Route(HttpMethod.GET, "/fonts/*.woff2"));

    private PublicRoutes() {
    }
}
