package br.com.erpcore.adapter.infra.security;

import java.util.HashMap;
import java.util.Map;

/**
 * Chaves da criptografia de campo, todas vindas do ambiente. As de cifrar são AES-256 (exatamente 32 bytes); mais de
 * uma pode estar ativa para permitir a troca: a {@code activeKeyId} cifra, todas decifram. A chave do índice cego
 * (HMAC) é separada e tem no mínimo 32 bytes; trocá-la exige reindexar os dados, então não tem rotação.
 */
public record FieldEncryptionSettings(String activeKeyId, Map<String, byte[]> encryptionKeys, byte[] blindIndexKey) {

    static final int ENCRYPTION_KEY_BYTES = 32;
    static final int MIN_BLIND_KEY_BYTES = 32;

    public FieldEncryptionSettings {
        Map<String, byte[]> copy = new HashMap<>();
        encryptionKeys.forEach((id, secret) -> {
            if (secret.length != ENCRYPTION_KEY_BYTES) {
                throw new IllegalArgumentException("encryption key " + id + " must have exactly 256 bits");
            }
            copy.put(id, secret.clone());
        });
        if (!copy.containsKey(activeKeyId)) {
            throw new IllegalArgumentException("active encryption key is not configured");
        }
        if (blindIndexKey.length < MIN_BLIND_KEY_BYTES) {
            throw new IllegalArgumentException("blind index key must have at least 256 bits");
        }
        encryptionKeys = Map.copyOf(copy);
        blindIndexKey = blindIndexKey.clone();
    }

    @Override
    public String toString() {
        return "FieldEncryptionSettings[redacted]";
    }
}
