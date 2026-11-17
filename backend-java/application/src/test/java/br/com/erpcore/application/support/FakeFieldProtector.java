package br.com.erpcore.application.support;

import br.com.erpcore.application.shared.port.out.FieldContext;
import br.com.erpcore.application.shared.port.out.FieldProtectionException;
import br.com.erpcore.application.shared.port.out.FieldProtector;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Substituto de teste: nada de criptografia real (isso é do adaptador Tink e dos testes dele). Só imita o contrato
 * que importa para a persistência: o texto gravado não é o valor, só decifra no mesmo contexto, e o índice cego é
 * determinístico e preso ao contexto.
 */
public final class FakeFieldProtector implements FieldProtector {

    private static final String PREFIX = "fake-cipher:";

    @Override
    public byte[] encrypt(FieldContext context, String plaintext) {
        String payload = context.associatedData() + "|" + plaintext;
        return (PREFIX + Base64.getEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8)))
                .getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String decrypt(FieldContext context, byte[] ciphertext) {
        String text = new String(ciphertext, StandardCharsets.UTF_8);
        if (!text.startsWith(PREFIX)) {
            throw new FieldProtectionException("not a fake ciphertext", null);
        }
        String payload = new String(Base64.getDecoder().decode(text.substring(PREFIX.length())), StandardCharsets.UTF_8);
        String expected = context.associatedData() + "|";
        if (!payload.startsWith(expected)) {
            throw new FieldProtectionException("wrong context", null);
        }
        return payload.substring(expected.length());
    }

    @Override
    public String blindIndex(FieldContext context, String normalizedValue) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(("blind|" + context.associatedData() + "|" + normalizedValue).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
