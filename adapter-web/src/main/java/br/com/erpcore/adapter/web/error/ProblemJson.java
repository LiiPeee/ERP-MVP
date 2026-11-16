package br.com.erpcore.adapter.web.error;

import br.com.erpcore.adapter.web.correlation.CorrelationIds;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Resposta de erro escrita pelos filtros (que ficam antes dos controllers). O texto é fixo e o correlation_id
 * só contém caracteres seguros, por isso não precisa de escape.
 */
public final class ProblemJson {

    private ProblemJson() {
    }

    public static void write(HttpServletResponse response, int status, String code, String title) throws IOException {
        response.setStatus(status);
        response.setContentType("application/problem+json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String body = "{\"title\":\"%s\",\"status\":%d,\"code\":\"%s\",\"correlationId\":\"%s\"}"
                .formatted(title, status, code, CorrelationIds.current());
        response.getWriter().write(body);
    }
}
