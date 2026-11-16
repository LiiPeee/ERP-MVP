package br.com.erpcore.adapter.persistence.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.ColumnTransformer;

@Entity
@Table(name = "audit_event")
public class AuditEventJpaEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "outcome", nullable = false)
    private String outcome;

    @Column(name = "correlation_id", nullable = false)
    private String correlationId;

    /** Coluna jsonb: o valor entra como texto JSON e é convertido pelo próprio PostgreSQL. */
    @Column(name = "details", nullable = false, columnDefinition = "jsonb")
    @ColumnTransformer(read = "details::text", write = "?::jsonb")
    private String details;

    protected AuditEventJpaEntity() {
    }

    public AuditEventJpaEntity(UUID id, Instant occurredAt, UUID companyId, UUID actorUserId, String eventType,
                               String outcome, String correlationId, String details) {
        this.id = id;
        this.occurredAt = occurredAt;
        this.companyId = companyId;
        this.actorUserId = actorUserId;
        this.eventType = eventType;
        this.outcome = outcome;
        this.correlationId = correlationId;
        this.details = details;
    }

    public UUID getId() {
        return id;
    }
}
