package br.com.erpcore.domain.identity;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public final class User {

    private final UserId id;
    private final Email email;
    private final DisplayName displayName;
    private final PasswordHash passwordHash;
    private final Instant createdAt;
    private int failedLoginCount;
    private Instant lockedUntil;

    private User(UserId id, Email email, DisplayName displayName, PasswordHash passwordHash,
                 Instant createdAt, int failedLoginCount, Instant lockedUntil) {
        this.id = id;
        this.email = email;
        this.displayName = displayName;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
        this.failedLoginCount = failedLoginCount;
        this.lockedUntil = lockedUntil;
    }

    public static User register(UserId id, Email email, DisplayName displayName, PasswordHash passwordHash, Instant now) {
        return new User(id, email, displayName, passwordHash, now, 0, null);
    }

    /** Reconstrói um usuário já existente a partir do estado persistido. */
    public static User restore(UserId id, Email email, DisplayName displayName, PasswordHash passwordHash,
                               Instant createdAt, int failedLoginCount, Instant lockedUntil) {
        return new User(id, email, displayName, passwordHash, createdAt, failedLoginCount, lockedUntil);
    }

    public boolean isLockedAt(Instant instant) {
        return lockedUntil != null && instant.isBefore(lockedUntil);
    }

    public void registerFailedLogin(Instant now, LoginLockoutPolicy policy) {
        failedLoginCount++;
        Duration lock = policy.lockDurationAfter(failedLoginCount);
        if (!lock.isZero()) {
            lockedUntil = now.plus(lock);
        }
    }

    public void registerSuccessfulLogin() {
        failedLoginCount = 0;
        lockedUntil = null;
    }

    public UserId id() {
        return id;
    }

    public Email email() {
        return email;
    }

    public DisplayName displayName() {
        return displayName;
    }

    public PasswordHash passwordHash() {
        return passwordHash;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public int failedLoginCount() {
        return failedLoginCount;
    }

    public Optional<Instant> lockedUntil() {
        return Optional.ofNullable(lockedUntil);
    }
}
