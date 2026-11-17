package br.com.erpcore.domain.identity;

import br.com.erpcore.domain.shared.DomainValidationException;
import br.com.erpcore.domain.shared.Validation;
import java.util.Locale;
import java.util.regex.Pattern;

public record Email(String value) {

    public static final int MAX_LENGTH = 254;

    private static final String FIELD = "email";
    private static final Pattern SHAPE = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public Email {
        value = Validation.requireTrimmedText(FIELD, value, MAX_LENGTH).toLowerCase(Locale.ROOT);
        if (!SHAPE.matcher(value).matches()) {
            throw new DomainValidationException(FIELD, Validation.INVALID_FORMAT);
        }
    }

    public static Email of(String raw) {
        return new Email(raw);
    }

    @Override
    public String toString() {
        return "Email[redacted]";
    }
}
