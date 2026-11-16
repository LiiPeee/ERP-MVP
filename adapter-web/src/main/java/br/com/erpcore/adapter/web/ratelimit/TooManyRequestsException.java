package br.com.erpcore.adapter.web.ratelimit;

import java.time.Duration;

public final class TooManyRequestsException extends RuntimeException {

    private final transient Duration retryAfter;

    public TooManyRequestsException(Duration retryAfter) {
        super("Too many requests");
        this.retryAfter = retryAfter;
    }

    public long retryAfterSeconds() {
        return Math.max(1, retryAfter.toSeconds());
    }
}
