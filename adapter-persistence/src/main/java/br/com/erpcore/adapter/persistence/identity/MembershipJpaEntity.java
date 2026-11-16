package br.com.erpcore.adapter.persistence.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Table(name = "company_membership")
@DynamicUpdate
public class MembershipJpaEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "role", nullable = false, updatable = false)
    private String role;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected MembershipJpaEntity() {
    }

    public MembershipJpaEntity(UUID id, UUID companyId, UUID userId, String role, Instant createdAt) {
        this.id = id;
        this.companyId = companyId;
        this.userId = userId;
        this.role = role;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getRole() {
        return role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
