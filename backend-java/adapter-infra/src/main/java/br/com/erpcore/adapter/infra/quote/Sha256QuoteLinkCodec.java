package br.com.erpcore.adapter.infra.quote;

import br.com.erpcore.application.quote.port.out.NewQuoteLinkSecret;
import br.com.erpcore.application.quote.port.out.QuoteLinkCodec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/** Token do link com 256 bits aleatórios; no banco entra só o SHA-256 dele (sem sal: a entropia já é alta). */
public final class Sha256QuoteLinkCodec implements QuoteLinkCodec {

    private static final int TOKEN_BYTES = 32;

    private final SecureRandom random = new SecureRandom();

    @Override
    public NewQuoteLinkSecret generate() {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        String value = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return new NewQuoteLinkSecret(value, hash(value));
    }

    @Override
    public String hash(String presentedToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(presentedToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is required by every Java platform", impossible);
        }
    }
}
