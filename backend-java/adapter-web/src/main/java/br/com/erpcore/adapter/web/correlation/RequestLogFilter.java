package br.com.erpcore.adapter.web.correlation;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Uma linha de log por requisição: método, caminho, status e duração. Nunca registra a query string, cabeçalhos
 * nem corpo. O correlation_id e o company_id saem no JSON porque estão no MDC quando a linha é escrita.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
final class RequestLogFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger("http_request");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long startedAt = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            long millis = (System.nanoTime() - startedAt) / 1_000_000;
            LOG.info("method={} path={} status={} duration_ms={}", request.getMethod(), request.getRequestURI(),
                    response.getStatus(), millis);
        }
    }
}
