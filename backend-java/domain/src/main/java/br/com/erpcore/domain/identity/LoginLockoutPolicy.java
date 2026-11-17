package br.com.erpcore.domain.identity;

import java.time.Duration;

/**
 * Bloqueio progressivo: a partir de {@code failureThreshold} falhas seguidas a conta fica bloqueada por
 * {@code baseLock}, e o bloqueio dobra a cada nova falha, até {@code maxLock}.
 */
public record LoginLockoutPolicy(int failureThreshold, Duration baseLock, Duration maxLock) {

    public static final LoginLockoutPolicy STANDARD =
            new LoginLockoutPolicy(5, Duration.ofMinutes(1), Duration.ofHours(1));

    /** Limite de dobras que mantém a conta dentro do intervalo de {@code long} antes de aplicar o teto. */
    private static final int MAX_DOUBLINGS = 30;

    public LoginLockoutPolicy {
        if (failureThreshold < 1) {
            throw new IllegalArgumentException("failureThreshold must be at least 1");
        }
        if (baseLock.isZero() || baseLock.isNegative()) {
            throw new IllegalArgumentException("baseLock must be positive");
        }
        if (maxLock.compareTo(baseLock) < 0) {
            throw new IllegalArgumentException("maxLock must not be shorter than baseLock");
        }
    }

    public Duration lockDurationAfter(int consecutiveFailures) {
        if (consecutiveFailures < failureThreshold) {
            return Duration.ZERO;
        }
        int doublings = Math.min(consecutiveFailures - failureThreshold, MAX_DOUBLINGS);
        Duration candidate = baseLock.multipliedBy(1L << doublings);
        return candidate.compareTo(maxLock) > 0 ? maxLock : candidate;
    }
}
