package br.com.erpcore.domain.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.regex.Pattern;

/** Quantidade de um item (peças ou horas): maior que zero, até 9999,999, com no máximo três casas decimais. */
public record Quantity(BigDecimal value) {

    public static final int SCALE = 3;
    public static final BigDecimal MAX = new BigDecimal("9999.999");

    private static final String FIELD = "quantity";
    private static final Pattern PLAIN_DECIMAL = Pattern.compile("^-?\\d+(\\.\\d+)?$");

    public Quantity {
        Objects.requireNonNull(value, FIELD);
        try {
            value = value.setScale(SCALE, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException tooManyDecimals) {
            throw new DomainValidationException(FIELD, Validation.INVALID_FORMAT);
        }
        if (value.signum() <= 0 || value.compareTo(MAX) > 0) {
            throw new DomainValidationException(FIELD, Validation.OUT_OF_RANGE);
        }
    }

    public static Quantity of(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new DomainValidationException(FIELD, Validation.REQUIRED);
        }
        String text = raw.trim();
        if (!PLAIN_DECIMAL.matcher(text).matches()) {
            throw new DomainValidationException(FIELD, Validation.INVALID_FORMAT);
        }
        return new Quantity(new BigDecimal(text));
    }

    public static Quantity of(int whole) {
        return new Quantity(BigDecimal.valueOf(whole));
    }

    @Override
    public String toString() {
        return value.toPlainString();
    }
}
