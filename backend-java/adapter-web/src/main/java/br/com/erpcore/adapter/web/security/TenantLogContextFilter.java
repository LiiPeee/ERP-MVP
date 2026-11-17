package br.com.erpcore.adapter.web.security;

import br.com.erpcore.adapter.web.correlation.CorrelationIds;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

/** Depois da autenticação, põe o company_id (que veio do token) em todas as linhas de log da requisição. */
final class TenantLogContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken authentication) {
            MDC.put(CorrelationIds.COMPANY_MDC_KEY, authentication.getToken().getClaimAsString(TokenClaims.COMPANY));
        }
        chain.doFilter(request, response);
    }
}
