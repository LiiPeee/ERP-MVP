package br.com.erpcore.application.quote;

import java.time.Instant;
import java.util.UUID;

/**
 * O resultado de enviar (ou reenviar). O link só existe aqui: no banco fica só o hash. {@code delivered} é falso se
 * o canal de entrega falhou; o orçamento continua enviado e o dono pode reenviar.
 */
public record QuoteSentResult(UUID quoteId, String publicUrl, Instant expiresAt, boolean delivered) {

    @Override
    public String toString() {
        return "QuoteSentResult[quoteId=" + quoteId + "]";
    }
}
