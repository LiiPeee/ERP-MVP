package br.com.erpcore.adapter.web.request;

import br.com.erpcore.adapter.web.error.ProblemJson;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Recusa corpo grande ou sem tamanho declarado antes de qualquer processamento (a verificação de senha com
 * Argon2 é cara e o JSON não tem teto próprio). O corpo legítimo da API tem poucas centenas de bytes.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
final class RequestSizeLimitFilter extends OncePerRequestFilter {

    static final long MAX_BODY_BYTES = 16 * 1024;

    private static final int PAYLOAD_TOO_LARGE = 413;

    private static final Set<String> METHODS_WITH_BODY = Set.of("POST", "PUT", "PATCH");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !METHODS_WITH_BODY.contains(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long declared = request.getContentLengthLong();
        if (declared > MAX_BODY_BYTES) {
            ProblemJson.write(response, PAYLOAD_TOO_LARGE, "PAYLOAD_TOO_LARGE", "Corpo da requisição grande demais");
            return;
        }
        if (declared < 0 && request.getHeader("Transfer-Encoding") != null) {
            ProblemJson.write(response, HttpServletResponse.SC_LENGTH_REQUIRED, "LENGTH_REQUIRED",
                    "Informe o tamanho do corpo da requisição");
            return;
        }
        chain.doFilter(request, response);
    }
}
