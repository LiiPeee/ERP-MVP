package br.com.erpcore.domain.audit;

import br.com.erpcore.domain.company.CompanyId;
import br.com.erpcore.domain.identity.UserId;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Registro imutável de uma ação. Os {@code details} carregam só códigos técnicos (por exemplo, o motivo
 * de uma falha), nunca dado pessoal.
 */
public final class AuditEvent {

    private final AuditEventId id;
    private final Instant occurredAt;
    private final CompanyId companyId;
    private final UserId actorUserId;
    private final AuditEventType type;
    private final AuditOutcome outcome;
    private final String correlationId;
    private final Map<String, String> details;

    public AuditEvent(AuditEventId id, Instant occurredAt, CompanyId companyId, UserId actorUserId,
                      AuditEventType type, AuditOutcome outcome, String correlationId, Map<String, String> details) {
        this.id = Objects.requireNonNull(id, "id");
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
        this.companyId = companyId;
        this.actorUserId = actorUserId;
        this.type = Objects.requireNonNull(type, "type");
        this.outcome = Objects.requireNonNull(outcome, "outcome");
        this.correlationId = Objects.requireNonNull(correlationId, "correlationId");
        this.details = Map.copyOf(details);
    }

    public AuditEventId id() {
        return id;
    }

    public Instant occurredAt() {
        return occurredAt;
    }

    public Optional<CompanyId> companyId() {
        return Optional.ofNullable(companyId);
    }

    public Optional<UserId> actorUserId() {
        return Optional.ofNullable(actorUserId);
    }

    public AuditEventType type() {
        return type;
    }

    public AuditOutcome outcome() {
        return outcome;
    }

    public String correlationId() {
        return correlationId;
    }

    public Map<String, String> details() {
        return details;
    }
}
