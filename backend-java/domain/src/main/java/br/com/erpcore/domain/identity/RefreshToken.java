package br.com.erpcore.domain.identity;

import br.com.erpcore.domain.company.CompanyId;
import java.time.Instant;
import java.util.Optional;

public final class RefreshToken {

    private final RefreshTokenId id;
    private final UserId userId;
    private final CompanyId companyId;
    private final TokenFamilyId familyId;
    private final TokenHash tokenHash;
    private final Instant issuedAt;
    private final Instant expiresAt;
    private final Instant familyStartedAt;
    private Instant revokedAt;

    private RefreshToken(RefreshTokenId id, UserId userId, CompanyId companyId, TokenFamilyId familyId,
                         TokenHash tokenHash, Instant issuedAt, Instant expiresAt, Instant familyStartedAt,
                         Instant revokedAt) {
        this.id = id;
        this.userId = userId;
        this.companyId = companyId;
        this.familyId = familyId;
        this.tokenHash = tokenHash;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.familyStartedAt = familyStartedAt;
        this.revokedAt = revokedAt;
    }

    /** Primeiro token de uma sessão nova (login). */
    public static RefreshToken first(RefreshTokenId id, UserId userId, CompanyId companyId, TokenFamilyId familyId,
                                     TokenHash tokenHash, Instant now, SessionPolicy policy) {
        return new RefreshToken(id, userId, companyId, familyId, tokenHash, now,
                policy.refreshTokenExpiry(now, now), now, null);
    }

    public static RefreshToken restore(RefreshTokenId id, UserId userId, CompanyId companyId, TokenFamilyId familyId,
                                       TokenHash tokenHash, Instant issuedAt, Instant expiresAt,
                                       Instant familyStartedAt, Instant revokedAt) {
        return new RefreshToken(id, userId, companyId, familyId, tokenHash, issuedAt, expiresAt,
                familyStartedAt, revokedAt);
    }

    /** Troca este token por um novo na mesma sessão; este deixa de valer. */
    public RefreshToken rotateTo(RefreshTokenId newId, TokenHash newHash, Instant now, SessionPolicy policy) {
        if (isRevoked() || isExpiredAt(now)) {
            throw new IllegalStateException("only an active refresh token can be rotated");
        }
        revoke(now);
        return new RefreshToken(newId, userId, companyId, familyId, newHash, now,
                policy.refreshTokenExpiry(now, familyStartedAt), familyStartedAt, null);
    }

    public void revoke(Instant now) {
        if (revokedAt == null) {
            revokedAt = now;
        }
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpiredAt(Instant instant) {
        return !instant.isBefore(expiresAt);
    }

    public RefreshTokenId id() {
        return id;
    }

    public UserId userId() {
        return userId;
    }

    public CompanyId companyId() {
        return companyId;
    }

    public TokenFamilyId familyId() {
        return familyId;
    }

    public TokenHash tokenHash() {
        return tokenHash;
    }

    public Instant issuedAt() {
        return issuedAt;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Instant familyStartedAt() {
        return familyStartedAt;
    }

    public Optional<Instant> revokedAt() {
        return Optional.ofNullable(revokedAt);
    }
}
