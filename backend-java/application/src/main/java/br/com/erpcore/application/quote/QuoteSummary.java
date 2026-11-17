package br.com.erpcore.application.quote;

import java.time.Instant;
import java.util.UUID;

/** Uma linha da fila de orçamentos. */
public record QuoteSummary(UUID id, String status, String contactName, String vehicleLabel, String total,
                           Instant createdAt, Instant sentAt, Instant expiresAt) {

    @Override
    public String toString() {
        return "QuoteSummary[id=" + id + ", status=" + status + "]";
    }
}
