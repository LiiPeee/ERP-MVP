package br.com.erpcore.application.identity.port.out;

import java.time.Instant;

public record IssuedAccessToken(String value, Instant expiresAt) {

    @Override
    public String toString() {
        return "IssuedAccessToken[redacted]";
    }
}
