package br.com.erpcore.domain.identity;

import static br.com.erpcore.domain.identity.UserTestBuilder.aUser;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class UserTest {

    private static final Instant NOW = Instant.parse("2026-01-15T12:00:00Z");
    private static final LoginLockoutPolicy POLICY = new LoginLockoutPolicy(3, Duration.ofMinutes(1), Duration.ofMinutes(10));

    @Test
    void givenANewlyRegisteredUser_whenInspecting_thenItHasNoFailuresAndIsNotLocked() {
        User user = aUser().build();

        assertThat(user.failedLoginCount()).isZero();
        assertThat(user.lockedUntil()).isEmpty();
        assertThat(user.isLockedAt(NOW)).isFalse();
        assertThat(user.createdAt()).isEqualTo(UserTestBuilder.CREATED_AT);
    }

    @Test
    void givenFailuresBelowTheThreshold_whenTheyHappen_thenTheUserStaysUnlocked() {
        User user = aUser().build();

        user.registerFailedLogin(NOW, POLICY);
        user.registerFailedLogin(NOW, POLICY);

        assertThat(user.failedLoginCount()).isEqualTo(2);
        assertThat(user.isLockedAt(NOW)).isFalse();
    }

    @Test
    void givenTheThresholdIsReached_whenTheLastFailureHappens_thenTheUserIsLockedForTheBaseDuration() {
        User user = failTimes(aUser().build(), 3);

        assertThat(user.lockedUntil()).contains(NOW.plus(Duration.ofMinutes(1)));
        assertThat(user.isLockedAt(NOW.plusSeconds(59))).isTrue();
    }

    @Test
    void givenALockThatExpired_whenCheckingAtTheExpiryInstant_thenTheUserIsNoLongerLocked() {
        User user = failTimes(aUser().build(), 3);

        assertThat(user.isLockedAt(NOW.plus(Duration.ofMinutes(1)))).isFalse();
    }

    @Test
    void givenAnotherFailureAfterTheLockExpired_whenItHappens_thenTheLockIsLonger() {
        User user = failTimes(aUser().build(), 3);
        Instant afterFirstLock = NOW.plus(Duration.ofMinutes(2));

        user.registerFailedLogin(afterFirstLock, POLICY);

        assertThat(user.lockedUntil()).contains(afterFirstLock.plus(Duration.ofMinutes(2)));
    }

    @Test
    void givenASuccessfulLogin_whenItHappens_thenFailuresAndLockAreCleared() {
        User user = failTimes(aUser().build(), 3);

        user.registerSuccessfulLogin();

        assertThat(user.failedLoginCount()).isZero();
        assertThat(user.lockedUntil()).isEmpty();
    }

    @Test
    void givenPersistedState_whenRestoring_thenEveryFieldIsPreserved() {
        Instant lockedUntil = NOW.plusSeconds(30);
        User original = aUser().build();

        User restored = User.restore(original.id(), original.email(), original.displayName(),
                original.passwordHash(), original.createdAt(), 4, lockedUntil);

        assertThat(restored.id()).isEqualTo(original.id());
        assertThat(restored.email()).isEqualTo(original.email());
        assertThat(restored.displayName()).isEqualTo(original.displayName());
        assertThat(restored.passwordHash()).isEqualTo(original.passwordHash());
        assertThat(restored.failedLoginCount()).isEqualTo(4);
        assertThat(restored.lockedUntil()).contains(lockedUntil);
    }

    private static User failTimes(User user, int times) {
        for (int attempt = 0; attempt < times; attempt++) {
            user.registerFailedLogin(NOW, POLICY);
        }
        return user;
    }
}
