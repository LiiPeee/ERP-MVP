package br.com.erpcore.adapter.persistence.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.DynamicUpdate;

/** "user" é palavra reservada no PostgreSQL, por isso o nome da tabela vai entre aspas. */
@Entity
@Table(name = "\"user\"")
@DynamicUpdate
public class UserJpaEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "email", nullable = false, updatable = false)
    private String email;

    @Column(name = "display_name", nullable = false, updatable = false)
    private String displayName;

    @Column(name = "password_hash", nullable = false, updatable = false)
    private String passwordHash;

    @Column(name = "failed_login_count", nullable = false)
    private int failedLoginCount;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected UserJpaEntity() {
    }

    public UserJpaEntity(UUID id, String email, String displayName, String passwordHash, int failedLoginCount,
                         Instant lockedUntil, Instant createdAt) {
        this.id = id;
        this.email = email;
        this.displayName = displayName;
        this.passwordHash = passwordHash;
        this.failedLoginCount = failedLoginCount;
        this.lockedUntil = lockedUntil;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public int getFailedLoginCount() {
        return failedLoginCount;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
