package br.com.erpcore.domain.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Dinheiro em reais, sempre com duas casas decimais ({@code NUMERIC(18,2)} no banco).
 * <p>
 * Regra de arredondamento, a única do produto: o que é digitado e exato (mais de duas casas é recusado, nunca
 * arredondado em silêncio); o que é calculado (quantidade vezes preço) é arredondado para o centavo com
 * {@link RoundingMode#HALF_UP}, ou seja, meio centavo sobe.
 */
public record Money(BigDecimal amount) implements Comparable<Money> {

    public static final int SCALE = 2;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private static final String FIELD = "amount";
    private static final Pattern PLAIN_DECIMAL = Pattern.compile("^-?\\d+(\\.\\d+)?$");
    private static final BigDecimal CENTS_PER_REAL = BigDecimal.valueOf(100);

    public Money {
        Objects.requireNonNull(amount, FIELD);
        try {
            amount = amount.setScale(SCALE, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException tooManyDecimals) {
            throw new DomainValidationException(FIELD, Validation.INVALID_FORMAT);
        }
    }

    public static Money of(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new DomainValidationException(FIELD, Validation.REQUIRED);
        }
        String text = raw.trim();
        if (!PLAIN_DECIMAL.matcher(text).matches()) {
            throw new DomainValidationException(FIELD, Validation.INVALID_FORMAT);
        }
        return new Money(new BigDecimal(text));
    }

    public static Money ofCents(long cents) {
        return new Money(BigDecimal.valueOf(cents).divide(CENTS_PER_REAL, SCALE, RoundingMode.UNNECESSARY));
    }

    /** Aplica a regra de arredondamento a um valor calculado. */
    public static Money rounded(BigDecimal computed) {
        return new Money(computed.setScale(SCALE, ROUNDING));
    }

    public static Money zero() {
        return new Money(BigDecimal.ZERO);
    }

    public Money add(Money other) {
        return new Money(amount.add(other.amount));
    }

    public Money multiply(Quantity quantity) {
        return rounded(amount.multiply(quantity.value()));
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    public boolean isPositive() {
        return amount.signum() > 0;
    }

    @Override
    public int compareTo(Money other) {
        return amount.compareTo(other.amount);
    }

    @Override
    public String toString() {
        return amount.toPlainString();
    }
}
