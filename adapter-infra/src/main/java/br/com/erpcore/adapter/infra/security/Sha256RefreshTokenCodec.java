package br.com.erpcore.adapter.infra.security;

import br.com.erpcore.application.identity.port.out.NewRefreshSecret;
import br.com.erpcore.application.identity.port.out.RefreshTokenCodec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/** Token de 256 bits aleatórios; no banco entra só o SHA-256 dele (sem sal: a entropia já é alta). */
public final class Sha256RefreshTokenCodec implements RefreshTokenCodec {

    private static final int TOKEN_BYTES = 32;

    private final SecureRandom random = new SecureRandom();

    @Override
    public NewRefreshSecret generate() {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        String value = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return new NewRefreshSecret(value, hash(value));
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
