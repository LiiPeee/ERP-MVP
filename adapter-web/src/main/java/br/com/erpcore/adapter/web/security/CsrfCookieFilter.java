package br.com.erpcore.adapter.web.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * O Spring carrega o token CSRF só quando alguém o pede. Este filtro o pede em toda resposta, para que o
 * cookie XSRF-TOKEN chegue ao navegador (o login, por exemplo) antes da primeira chamada a refresh ou logout.
 */
final class CsrfCookieFilter extends OncePerRequestFilter {

    /** O nome com que o Spring Security deixa o token (adiado) na requisição. */
    private static final String REQUEST_ATTRIBUTE = "_csrf";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (request.getAttribute(REQUEST_ATTRIBUTE) instanceof CsrfToken token) {
            token.getToken();
        }
        chain.doFilter(request, response);
    }
}
