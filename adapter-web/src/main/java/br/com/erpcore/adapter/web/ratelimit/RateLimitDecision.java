package br.com.erpcore.adapter.web.ratelimit;

import java.time.Duration;

public record RateLimitDecision(boolean allowed, Duration retryAfter) {

    static RateLimitDecision permitted() {
        return new RateLimitDecision(true, Duration.ZERO);
    }

    static RateLimitDecision refused(Duration retryAfter) {
        return new RateLimitDecision(false, retryAfter);
    }
}
