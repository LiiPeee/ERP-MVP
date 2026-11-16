package br.com.erpcore.adapter.infra.security;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuração do token de acesso. As chaves (HMAC-SHA256) vêm do ambiente; mais de uma pode estar ativa para
 * permitir a troca sem derrubar sessões: a {@code activeKeyId} assina, todas validam.
 */
public record JwtSettings(String issuer, String audience, Duration accessTokenLifetime, String activeKeyId,
                          Map<String, byte[]> keys) {

    static final int MIN_KEY_BYTES = 32;

    public JwtSettings {
        Map<String, byte[]> copy = new HashMap<>();
        keys.forEach((id, secret) -> {
            if (secret.length < MIN_KEY_BYTES) {
                throw new IllegalArgumentException("signing key " + id + " must have at least 256 bits");
            }
            copy.put(id, secret.clone());
        });
        if (!copy.containsKey(activeKeyId)) {
            throw new IllegalArgumentException("active signing key is not configured");
        }
        keys = Map.copyOf(copy);
    }

    @Override
    public String toString() {
        return "JwtSettings[redacted]";
    }
}
