package br.com.erpcore.domain.shared;

public final class Validation {

    public static final String REQUIRED = "REQUIRED";
    public static final String TOO_SHORT = "TOO_SHORT";
    public static final String TOO_LONG = "TOO_LONG";
    public static final String INVALID_FORMAT = "INVALID_FORMAT";

    private Validation() {
    }

    /** Aceita só texto não vazio (após aparar as bordas) com no máximo {@code maxLength} caracteres. */
    public static String requireTrimmedText(String field, String raw, int maxLength) {
        if (raw == null || raw.isBlank()) {
            throw new DomainValidationException(field, REQUIRED);
        }
        String trimmed = raw.trim();
        if (trimmed.length() > maxLength) {
            throw new DomainValidationException(field, TOO_LONG);
        }
        return trimmed;
    }
}
