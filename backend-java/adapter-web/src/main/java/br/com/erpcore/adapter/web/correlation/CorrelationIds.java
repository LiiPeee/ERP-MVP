package br.com.erpcore.adapter.web.correlation;

import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;

/** O identificador que liga todas as linhas de log e a auditoria de uma mesma requisição. */
public final class CorrelationIds {

    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlation_id";
    public static final String COMPANY_MDC_KEY = "company_id";

    private static final Pattern SAFE = Pattern.compile("[A-Za-z0-9-]{1,64}");

    private CorrelationIds() {
    }

    /** Aceita o que o cliente mandou só se for curto e sem caracteres especiais; senão, gera um novo. */
    static String acceptOrCreate(String fromClient) {
        return fromClient != null && SAFE.matcher(fromClient).matches() ? fromClient : UUID.randomUUID().toString();
    }

    public static String current() {
        String current = MDC.get(MDC_KEY);
        return current != null ? current : UUID.randomUUID().toString();
    }
}
