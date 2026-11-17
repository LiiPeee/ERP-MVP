package br.com.erpcore.adapter.persistence.quote;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Table(name = "quote")
@DynamicUpdate
public class QuoteJpaEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;

    @Column(name = "contact_id", nullable = false, updatable = false)
    private UUID contactId;

    @Column(name = "vehicle_id", nullable = false, updatable = false)
    private UUID vehicleId;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "validity_days", nullable = false, updatable = false)
    private int validityDays;

    @Column(name = "note", nullable = false, updatable = false)
    private String note;

    @Column(name = "items_revision", nullable = false)
    private int itemsRevision;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "decision_reason")
    private String decisionReason;

    protected QuoteJpaEntity() {
    }

    public QuoteJpaEntity(UUID id, UUID companyId, UUID contactId, UUID vehicleId, String status, int validityDays,
                          String note, int itemsRevision, Instant createdAt, Instant sentAt, Instant expiresAt,
                          Instant decidedAt, String decisionReason) {
        this.id = id;
        this.companyId = companyId;
        this.contactId = contactId;
        this.vehicleId = vehicleId;
        this.status = status;
        this.validityDays = validityDays;
        this.note = note;
        this.itemsRevision = itemsRevision;
        this.createdAt = createdAt;
        this.sentAt = sentAt;
        this.expiresAt = expiresAt;
        this.decidedAt = decidedAt;
        this.decisionReason = decisionReason;
    }

    /** Só o que o negócio muda depois de criado: o estado, a revisão dos itens e os momentos da decisão. */
    public void update(String status, int itemsRevision, Instant sentAt, Instant expiresAt, Instant decidedAt,
                       String decisionReason) {
        this.status = status;
        this.itemsRevision = itemsRevision;
        this.sentAt = sentAt;
        this.expiresAt = expiresAt;
        this.decidedAt = decidedAt;
        this.decisionReason = decisionReason;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public UUID getContactId() {
        return contactId;
    }

    public UUID getVehicleId() {
        return vehicleId;
    }

    public String getStatus() {
        return status;
    }

    public int getValidityDays() {
        return validityDays;
    }

    public String getNote() {
        return note;
    }

    public int getItemsRevision() {
        return itemsRevision;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    public String getDecisionReason() {
        return decisionReason;
    }
}
