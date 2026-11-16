package br.com.erpcore.application.support;

import br.com.erpcore.application.identity.port.out.NewRefreshSecret;
import br.com.erpcore.application.identity.port.out.RefreshTokenCodec;

public final class FakeRefreshTokenCodec implements RefreshTokenCodec {

    private static final String HASH_PREFIX = "hash-of-";

    private int generated;

    @Override
    public NewRefreshSecret generate() {
        generated++;
        String value = "refresh-secret-" + generated;
        return new NewRefreshSecret(value, hash(value));
    }

    @Override
    public String hash(String presentedToken) {
        return HASH_PREFIX + presentedToken;
    }
}
