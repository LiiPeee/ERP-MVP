package br.com.erpcore.domain.identity;

import java.time.Duration;
import java.time.Instant;

/**
 * Duração do refresh token e teto absoluto da sessão: por mais que se renove, uma sessão
 * nunca passa de {@code familyMaxAge} contado desde o login.
 */
public record SessionPolicy(Duration refreshTokenLifetime, Duration familyMaxAge) {

    public SessionPolicy {
        if (refreshTokenLifetime.isZero() || refreshTokenLifetime.isNegative()) {
            throw new IllegalArgumentException("refreshTokenLifetime must be positive");
        }
        if (familyMaxAge.compareTo(refreshTokenLifetime) < 0) {
            throw new IllegalArgumentException("familyMaxAge must not be shorter than refreshTokenLifetime");
        }
    }

    public Instant refreshTokenExpiry(Instant now, Instant familyStartedAt) {
        Instant byLifetime = now.plus(refreshTokenLifetime);
        Instant byFamilyLimit = familyStartedAt.plus(familyMaxAge);
        return byLifetime.isBefore(byFamilyLimit) ? byLifetime : byFamilyLimit;
    }
}
