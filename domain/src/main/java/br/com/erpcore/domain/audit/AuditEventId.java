package br.com.erpcore.domain.audit;

import java.util.Objects;
import java.util.UUID;

public record AuditEventId(UUID value) {

    public AuditEventId {
        Objects.requireNonNull(value, "value");
    }
}
