package br.com.erpcore.adapter.persistence.support;

import br.com.erpcore.application.shared.port.out.FieldContext;
import br.com.erpcore.application.shared.port.out.FieldProtector;
import br.com.erpcore.application.shared.port.out.FieldProtectionException;
import java.util.Arrays;

/**
 * Leitura e escrita das colunas cifradas. Ao regravar uma linha, o valor que não mudou mantém o texto cifrado que já
 * estava no banco (cada cifragem gera um resultado novo; regravar sem necessidade só trocaria bytes à toa).
 */
public final class FieldColumns {

    private final FieldProtector protector;

    public FieldColumns(FieldProtector protector) {
        this.protector = protector;
    }

    public byte[] encrypt(FieldContext context, String plaintext) {
        return protector.encrypt(context, plaintext);
    }

    public String decrypt(FieldContext context, byte[] ciphertext) {
        return protector.decrypt(context, ciphertext);
    }

    public String blindIndex(FieldContext context, String normalizedValue) {
        return protector.blindIndex(context, normalizedValue);
    }

    /** O texto cifrado atual se ele ainda representa o mesmo valor; senão, um novo. */
    public byte[] reuseOrEncrypt(FieldContext context, byte[] currentCiphertext, String plaintext) {
        if (currentCiphertext != null) {
            try {
                if (protector.decrypt(context, currentCiphertext).equals(plaintext)) {
                    return Arrays.copyOf(currentCiphertext, currentCiphertext.length);
                }
            } catch (FieldProtectionException unreadable) {
                // Chave antiga removida ou dado corrompido: regrava com a chave atual.
            }
        }
        return protector.encrypt(context, plaintext);
    }
}
