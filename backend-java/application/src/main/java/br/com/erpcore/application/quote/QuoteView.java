package br.com.erpcore.application.quote;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * O orçamento como a API o devolve ao dono. O estado é o de agora (enviado fora do prazo já aparece como vencido) e
 * o telefone do cliente vai mascarado.
 */
public record QuoteView(UUID id, String status, UUID contactId, String contactName, String contactPhoneMasked,
                        UUID vehicleId, String vehicleLabel, List<QuoteItemView> items, String total, String note,
                        int validityDays, Instant createdAt, Instant sentAt, Instant expiresAt, Instant decidedAt,
                        String decisionReason) {

    @Override
    public String toString() {
        return "QuoteView[id=" + id + ", status=" + status + "]";
    }
}
