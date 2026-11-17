package br.com.erpcore.application.support;

import br.com.erpcore.application.quote.port.out.NewQuoteLinkSecret;
import br.com.erpcore.application.quote.port.out.QuoteLinkCodec;
import java.util.UUID;

/** Valores únicos: cada token gerado é diferente e o hash dele é previsível para o teste. */
public final class FakeQuoteLinkCodec implements QuoteLinkCodec {

    private static final String HASH_PREFIX = "link-hash-of-";

    @Override
    public NewQuoteLinkSecret generate() {
        String value = "link-secret-" + UUID.randomUUID();
        return new NewQuoteLinkSecret(value, hash(value));
    }

    @Override
    public String hash(String presentedToken) {
        return HASH_PREFIX + presentedToken;
    }
}
