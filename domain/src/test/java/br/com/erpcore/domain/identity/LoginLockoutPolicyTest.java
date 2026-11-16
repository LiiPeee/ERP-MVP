package br.com.erpcore.domain.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LoginLockoutPolicyTest {

    private final LoginLockoutPolicy policy = new LoginLockoutPolicy(5, Duration.ofMinutes(1), Duration.ofHours(1));

    @ParameterizedTest
    @CsvSource({"0,0", "1,0", "4,0"})
    void givenFewerFailuresThanTheThreshold_whenComputingTheLock_thenThereIsNoLock(int failures, long expectedSeconds) {
        assertThat(policy.lockDurationAfter(failures)).isEqualTo(Duration.ofSeconds(expectedSeconds));
    }

    @ParameterizedTest
    @CsvSource({"5,60", "6,120", "7,240", "8,480", "9,960", "10,1920"})
    void givenFailuresReachingTheThreshold_whenComputingTheLock_thenItDoublesEachTime(int failures, long expectedSeconds) {
        assertThat(policy.lockDurationAfter(failures)).isEqualTo(Duration.ofSeconds(expectedSeconds));
    }

    @ParameterizedTest
    @CsvSource({"11,3600", "40,3600", "1000000,3600"})
    void givenManyFailures_whenComputingTheLock_thenItNeverExceedsTheMaximum(int failures, long expectedSeconds) {
        assertThat(policy.lockDurationAfter(failures)).isEqualTo(Duration.ofSeconds(expectedSeconds));
    }

    @Test
    void givenAThresholdBelowOne_whenCreatingThePolicy_thenItIsRejected() {
        assertThatThrownBy(() -> new LoginLockoutPolicy(0, Duration.ofMinutes(1), Duration.ofHours(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenANonPositiveBaseLock_whenCreatingThePolicy_thenItIsRejected() {
        assertThatThrownBy(() -> new LoginLockoutPolicy(5, Duration.ZERO, Duration.ofHours(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenAMaximumSmallerThanTheBase_whenCreatingThePolicy_thenItIsRejected() {
        assertThatThrownBy(() -> new LoginLockoutPolicy(5, Duration.ofMinutes(2), Duration.ofMinutes(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenTheStandardPolicy_whenReadingIt_thenItLocksFiveFailuresForOneMinuteUpToOneHour() {
        assertThat(LoginLockoutPolicy.STANDARD.lockDurationAfter(5)).isEqualTo(Duration.ofMinutes(1));
        assertThat(LoginLockoutPolicy.STANDARD.lockDurationAfter(500)).isEqualTo(Duration.ofHours(1));
    }
}
