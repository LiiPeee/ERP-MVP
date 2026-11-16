package br.com.erpcore.domain.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class SessionPolicyTest {

    private static final Instant NOW = Instant.parse("2026-02-01T10:00:00Z");

    @Test
    void givenTheFamilyLimitIsFar_whenComputingExpiry_thenTheTokenLifetimeApplies() {
        SessionPolicy policy = new SessionPolicy(Duration.ofDays(7), Duration.ofDays(30));

        assertThat(policy.refreshTokenExpiry(NOW, NOW)).isEqualTo(NOW.plus(Duration.ofDays(7)));
    }

    @Test
    void givenTheFamilyLimitIsNear_whenComputingExpiry_thenTheFamilyLimitApplies() {
        SessionPolicy policy = new SessionPolicy(Duration.ofDays(7), Duration.ofDays(30));
        Instant familyStart = NOW.minus(Duration.ofDays(29));

        assertThat(policy.refreshTokenExpiry(NOW, familyStart)).isEqualTo(familyStart.plus(Duration.ofDays(30)));
    }

    @Test
    void givenANonPositiveLifetime_whenCreatingThePolicy_thenItIsRejected() {
        assertThatThrownBy(() -> new SessionPolicy(Duration.ZERO, Duration.ofDays(30)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenAFamilyLimitShorterThanTheLifetime_whenCreatingThePolicy_thenItIsRejected() {
        assertThatThrownBy(() -> new SessionPolicy(Duration.ofDays(7), Duration.ofDays(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
