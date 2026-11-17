package br.com.erpcore.domain.shared;

import static br.com.erpcore.domain.shared.DomainValidationAssertions.assertRejectedWith;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class QuantityTest {

    private static final String FIELD = "quantity";

    @Test
    void givenIntegerOrDecimalText_whenCreating_thenItHasThreeDecimalPlaces() {
        assertThat(Quantity.of("2").value()).hasToString("2.000");
        assertThat(Quantity.of("1.5").value()).hasToString("1.500");
        assertThat(Quantity.of("0.001").value()).hasToString("0.001");
    }

    @Test
    void givenAnInteger_whenCreating_thenItIsAccepted() {
        assertThat(Quantity.of(4).value()).hasToString("4.000");
    }

    @Test
    void givenMoreThanThreeDecimalPlaces_whenCreating_thenItIsRejected() {
        assertRejectedWith(() -> Quantity.of("1.0001"), FIELD, "INVALID_FORMAT");
    }

    @Test
    void givenZeroOrNegative_whenCreating_thenItIsOutOfRange() {
        assertRejectedWith(() -> Quantity.of("0"), FIELD, "OUT_OF_RANGE");
        assertRejectedWith(() -> Quantity.of("-1"), FIELD, "OUT_OF_RANGE");
    }

    @Test
    void givenAboveTheMaximum_whenCreating_thenItIsOutOfRange() {
        assertThat(Quantity.of("9999.999").value()).hasToString("9999.999");
        assertRejectedWith(() -> Quantity.of("10000"), FIELD, "OUT_OF_RANGE");
    }

    @Test
    void givenNullBlankOrNotANumber_whenCreating_thenItIsRejected() {
        assertRejectedWith(() -> Quantity.of((String) null), FIELD, "REQUIRED");
        assertRejectedWith(() -> Quantity.of(" "), FIELD, "REQUIRED");
        assertRejectedWith(() -> Quantity.of("dois"), FIELD, "INVALID_FORMAT");
    }

    @Test
    void givenQuantitiesWrittenDifferently_whenComparing_thenTheyAreEqual() {
        assertThat(Quantity.of("2")).isEqualTo(Quantity.of("2.000"));
    }
}
