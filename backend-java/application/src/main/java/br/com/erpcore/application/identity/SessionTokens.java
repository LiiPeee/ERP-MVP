package br.com.erpcore.application.identity;

import java.time.Instant;

public record SessionTokens(String accessToken, Instant accessTokenExpiresAt, String refreshToken,
                            Instant refreshTokenExpiresAt) {

    @Override
    public String toString() {
        return "SessionTokens[redacted]";
    }
}
