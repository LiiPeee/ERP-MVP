package br.com.erpcore.adapter.web.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * O link do orçamento leva o token no endereço: nada dele pode ficar em cache de navegador ou de proxy, ser
 * indexado por buscador, nem vazar pelo cabeçalho Referer (a política da cadeia de segurança já é no-referrer).
 * Vale para a API pública e para a página do cliente, inclusive nas respostas de erro.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 3)
final class PublicLinkHeadersFilter extends OncePerRequestFilter {

    private static final String PUBLIC_API = "/api/public/";
    private static final String CLIENT_PAGE = "/o/";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !(path.startsWith(PUBLIC_API) || path.startsWith(CLIENT_PAGE));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.setHeader(HttpHeaders.PRAGMA, "no-cache");
        response.setHeader("X-Robots-Tag", "noindex, nofollow, noarchive");
        chain.doFilter(request, response);
    }
}
