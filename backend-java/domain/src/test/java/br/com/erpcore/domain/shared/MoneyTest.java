package br.com.erpcore.domain.shared;

import static br.com.erpcore.domain.shared.DomainValidationAssertions.assertRejectedWith;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {

    private static final String FIELD = "amount";

    @Test
    void givenAnIntegerAmount_whenCreating_thenItHasTwoDecimalPlaces() {
        assertThat(Money.of("10").amount()).isEqualByComparingTo("10.00").hasScaleOf(2);
        assertThat(Money.of("10.5").amount()).hasToString("10.50");
    }

    @Test
    void givenMoreThanTwoDecimalPlaces_whenCreating_thenItIsRejectedInsteadOfSilentlyRounded() {
        assertRejectedWith(() -> Money.of("10.555"), FIELD, "INVALID_FORMAT");
    }

    @Test
    void givenNullBlankOrNotANumber_whenCreating_thenItIsRejected() {
        assertRejectedWith(() -> Money.of((String) null), FIELD, "REQUIRED");
        assertRejectedWith(() -> Money.of("  "), FIELD, "REQUIRED");
        assertRejectedWith(() -> Money.of("abc"), FIELD, "INVALID_FORMAT");
        assertRejectedWith(() -> Money.of("1e3"), FIELD, "INVALID_FORMAT");
    }

    @Test
    void givenCents_whenCreating_thenItIsTheSameAsTheDecimalAmount() {
        assertThat(Money.ofCents(12_345)).isEqualTo(Money.of("123.45"));
        assertThat(Money.ofCents(5).amount()).hasToString("0.05");
    }

    @Test
    void givenTwoAmounts_whenAdding_thenTheSumIsExact() {
        assertThat(Money.of("0.10").add(Money.of("0.20"))).isEqualTo(Money.of("0.30"));
    }

    @Test
    void givenAmountsWithDifferentTextualScale_whenComparing_thenTheyAreEqual() {
        assertThat(Money.of("5")).isEqualTo(Money.of("5.00")).hasSameHashCodeAs(Money.of("5.00"));
    }

    @Test
    void givenSignedAmounts_whenAsked_thenZeroPositiveAndNegativeAreTold() {
        assertThat(Money.zero().isZero()).isTrue();
        assertThat(Money.of("0.01").isPositive()).isTrue();
        assertThat(Money.of("-0.01").isPositive()).isFalse();
        assertThat(Money.of("-0.01").isZero()).isFalse();
    }

    @Test
    void givenTwoAmounts_whenComparing_thenTheOrderFollowsTheValue() {
        assertThat(Money.of("2.00")).isGreaterThan(Money.of("1.99")).isLessThan(Money.of("2.01"));
    }

    @Test
    void givenAQuantity_whenMultiplying_thenTheResultIsRoundedHalfUpToCents() {
        assertThat(Money.of("10.00").multiply(Quantity.of("1.5"))).isEqualTo(Money.of("15.00"));
        assertThat(Money.of("19.99").multiply(Quantity.of("2.5"))).isEqualTo(Money.of("49.98")); // 49,975
        assertThat(Money.of("0.01").multiply(Quantity.of("0.5"))).isEqualTo(Money.of("0.01")); // 0,005
        assertThat(Money.of("0.01").multiply(Quantity.of("0.4"))).isEqualTo(Money.of("0.00")); // 0,004
        assertThat(Money.of("33.33").multiply(Quantity.of("3"))).isEqualTo(Money.of("99.99"));
    }

    @Test
    void givenABigDecimalWithManyPlaces_whenRounding_thenItFollowsTheDocumentedRule() {
        assertThat(Money.rounded(new BigDecimal("2.345"))).isEqualTo(Money.of("2.35"));
        assertThat(Money.rounded(new BigDecimal("2.344"))).isEqualTo(Money.of("2.34"));
        assertThat(Money.rounded(new BigDecimal("-2.345"))).isEqualTo(Money.of("-2.35"));
    }

    @Test
    void givenAnAmount_whenPrinting_thenItIsPlain() {
        assertThat(Money.of("1234.5")).hasToString("1234.50");
    }
}
