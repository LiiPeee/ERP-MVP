package br.com.erpcore.application.support;

import br.com.erpcore.application.identity.port.out.NewRefreshSecret;
import br.com.erpcore.application.identity.port.out.RefreshTokenCodec;
import java.util.UUID;

/** Valores únicos: vários contextos de teste podem dividir o mesmo banco sem colidir no hash. */
public final class FakeRefreshTokenCodec implements RefreshTokenCodec {

    private static final String HASH_PREFIX = "hash-of-";

    @Override
    public NewRefreshSecret generate() {
        String value = "refresh-secret-" + UUID.randomUUID();
        return new NewRefreshSecret(value, hash(value));
    }

    @Override
    public String hash(String presentedToken) {
        return HASH_PREFIX + presentedToken;
    }
}
