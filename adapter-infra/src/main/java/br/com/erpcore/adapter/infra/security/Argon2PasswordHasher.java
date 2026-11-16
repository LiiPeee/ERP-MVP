package br.com.erpcore.adapter.infra.security;

import br.com.erpcore.application.identity.port.out.PasswordHasher;
import java.util.UUID;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Argon2id com custo mínimo recomendado pela OWASP (19 MiB, 2 iterações, 1 thread). */
public final class Argon2PasswordHasher implements PasswordHasher {

    static final int SALT_BYTES = 16;
    static final int HASH_BYTES = 32;
    static final int PARALLELISM = 1;
    static final int MEMORY_KIB = 19_456;
    static final int ITERATIONS = 2;

    private final PasswordEncoder encoder;
    private final String placeholderHash;

    public Argon2PasswordHasher() {
        this(new Argon2PasswordEncoder(SALT_BYTES, HASH_BYTES, PARALLELISM, MEMORY_KIB, ITERATIONS));
    }

    Argon2PasswordHasher(PasswordEncoder encoder) {
        this.encoder = encoder;
        this.placeholderHash = encoder.encode(UUID.randomUUID().toString());
    }

    @Override
    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String hash) {
        try {
            return encoder.matches(rawPassword, hash);
        } catch (IllegalArgumentException malformedHash) {
            return false;
        }
    }

    @Override
    public void spendVerificationTime(String rawPassword) {
        encoder.matches(rawPassword, placeholderHash);
    }
}
