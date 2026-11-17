package br.com.erpcore.adapter.persistence.quote;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Table(name = "quote_public_link")
@DynamicUpdate
public class QuoteLinkJpaEntity {

    @Id
    @Column(name = "token_hash")
    private String tokenHash;

    @Column(name = "quote_id", nullable = false, updatable = false)
    private UUID quoteId;

    @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    protected QuoteLinkJpaEntity() {
    }

    public QuoteLinkJpaEntity(String tokenHash, UUID quoteId, UUID companyId, Instant issuedAt, Instant expiresAt,
                              Instant decidedAt) {
        this.tokenHash = tokenHash;
        this.quoteId = quoteId;
        this.companyId = companyId;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.decidedAt = decidedAt;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public UUID getQuoteId() {
        return quoteId;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }
}
