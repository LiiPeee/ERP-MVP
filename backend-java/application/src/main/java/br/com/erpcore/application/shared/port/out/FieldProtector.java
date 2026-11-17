package br.com.erpcore.application.shared.port.out;

/**
 * Criptografia de campo para dado pessoal (telefone, e-mail, placa). A cifra é autenticada e presa ao contexto
 * (empresa e campo): um texto cifrado copiado para outra empresa ou outro campo não decifra.
 * <p>
 * O índice cego permite buscar por valor exato sem decifrar nem guardar o valor: é um HMAC do valor já normalizado,
 * também preso ao contexto, com chave própria (diferente da de cifrar).
 */
public interface FieldProtector {

    /** Cifra. Cada chamada gera um resultado diferente, mesmo para o mesmo texto. */
    byte[] encrypt(FieldContext context, String plaintext);

    /** Decifra. Falha com {@link FieldProtectionException} se o texto foi adulterado ou é de outro contexto. */
    String decrypt(FieldContext context, byte[] ciphertext);

    /** O mesmo valor no mesmo contexto dá sempre o mesmo índice (64 caracteres hexadecimais). */
    String blindIndex(FieldContext context, String normalizedValue);
}
