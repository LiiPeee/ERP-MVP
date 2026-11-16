package br.com.erpcore.adapter.web.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class InMemoryRateLimiterTest {

    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final MutableClock clock = new MutableClock(Instant.parse("2026-08-01T10:00:00Z"));
    private final InMemoryRateLimiter limiter = new InMemoryRateLimiter(clock, 100);

    @Test
    void givenARequestWithinTheLimit_whenAcquiring_thenItIsAllowed() {
        assertThat(limiter.tryAcquire("a", 3, WINDOW).allowed()).isTrue();
        assertThat(limiter.tryAcquire("a", 3, WINDOW).allowed()).isTrue();
        assertThat(limiter.tryAcquire("a", 3, WINDOW).allowed()).isTrue();
    }

    @Test
    void givenTheLimitWasReached_whenAcquiringAgain_thenItIsRefusedWithTheTimeToWait() {
        for (int attempt = 0; attempt < 3; attempt++) {
            limiter.tryAcquire("a", 3, WINDOW);
        }
        clock.advance(Duration.ofSeconds(20));

        RateLimitDecision decision = limiter.tryAcquire("a", 3, WINDOW);

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.retryAfter()).isEqualTo(Duration.ofSeconds(40));
    }

    @Test
    void givenTheWindowPassed_whenAcquiringAgain_thenTheCounterStartsOver() {
        for (int attempt = 0; attempt < 4; attempt++) {
            limiter.tryAcquire("a", 3, WINDOW);
        }
        clock.advance(WINDOW);

        assertThat(limiter.tryAcquire("a", 3, WINDOW).allowed()).isTrue();
    }

    @Test
    void givenTwoKeys_whenOneIsExhausted_thenTheOtherIsNotAffected() {
        for (int attempt = 0; attempt < 4; attempt++) {
            limiter.tryAcquire("a", 3, WINDOW);
        }

        assertThat(limiter.tryAcquire("b", 3, WINDOW).allowed()).isTrue();
    }

    @Test
    void givenTooManyDistinctKeys_whenAcquiringANewOne_thenExpiredEntriesAreDroppedFirst() {
        InMemoryRateLimiter small = new InMemoryRateLimiter(clock, 2);
        small.tryAcquire("a", 1, WINDOW);
        small.tryAcquire("b", 1, WINDOW);
        clock.advance(WINDOW);

        assertThat(small.tryAcquire("c", 1, WINDOW).allowed()).isTrue();
        assertThat(small.trackedKeys()).isLessThanOrEqualTo(2);
    }

    @Test
    void givenTheKeyTableIsFullOfLiveEntries_whenAcquiringANewKey_thenItIsRefusedInsteadOfGrowingMemory() {
        InMemoryRateLimiter small = new InMemoryRateLimiter(clock, 2);
        small.tryAcquire("a", 5, WINDOW);
        small.tryAcquire("b", 5, WINDOW);

        assertThat(small.tryAcquire("c", 5, WINDOW).allowed()).isFalse();
        assertThat(small.trackedKeys()).isEqualTo(2);
        assertThat(small.tryAcquire("a", 5, WINDOW).allowed()).isTrue();
    }

    private static final class MutableClock extends Clock {

        private Instant now;

        MutableClock(Instant start) {
            this.now = start;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}
