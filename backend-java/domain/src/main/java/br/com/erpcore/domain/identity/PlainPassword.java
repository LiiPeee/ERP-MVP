package br.com.erpcore.domain.identity;

import br.com.erpcore.domain.shared.DomainValidationException;
import br.com.erpcore.domain.shared.Validation;

/** Senha em texto puro. Vive só o tempo de uma requisição e nunca aparece em texto de log. */
public final class PlainPassword {

    public static final int MIN_LENGTH = 12;
    public static final int MAX_LENGTH = 128;

    private static final String FIELD = "password";

    private final String value;

    private PlainPassword(String value) {
        this.value = value;
    }

    /** Senha escolhida no cadastro: aplica a política de tamanho. */
    public static PlainPassword chooseNew(String raw) {
        requireNotEmpty(raw);
        if (raw.length() < MIN_LENGTH) {
            throw new DomainValidationException(FIELD, Validation.TOO_SHORT);
        }
        requireWithinMaximum(raw);
        return new PlainPassword(raw);
    }

    /** Senha digitada no login: só limita o tamanho, para não revelar a política de quem erra. */
    public static PlainPassword ofLoginAttempt(String raw) {
        requireNotEmpty(raw);
        requireWithinMaximum(raw);
        return new PlainPassword(raw);
    }

    public String value() {
        return value;
    }

    private static void requireNotEmpty(String raw) {
        if (raw == null || raw.isEmpty()) {
            throw new DomainValidationException(FIELD, Validation.REQUIRED);
        }
    }

    private static void requireWithinMaximum(String raw) {
        if (raw.length() > MAX_LENGTH) {
            throw new DomainValidationException(FIELD, Validation.TOO_LONG);
        }
    }

    @Override
    public String toString() {
        return "PlainPassword[redacted]";
    }
}
