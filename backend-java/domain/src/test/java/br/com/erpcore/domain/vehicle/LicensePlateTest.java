package br.com.erpcore.domain.vehicle;

import static br.com.erpcore.domain.shared.DomainValidationAssertions.assertRejectedWith;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class LicensePlateTest {

    private static final String FIELD = "licensePlate";

    @ParameterizedTest
    @CsvSource({"abc1234,ABC1234", "ABC-1234,ABC1234", " abc 1234 ,ABC1234", "bra2e19,BRA2E19", "BRA-2E19,BRA2E19"})
    void givenAPlateInTheOldOrMercosulFormat_whenCreating_thenItIsNormalized(String raw, String expected) {
        assertThat(LicensePlate.of(raw).value()).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"AB12345", "ABCD123", "1234ABC", "ABC123", "ABC-12345", "ABC12E4", "ÁBC1234", "ABC 12 34 5"})
    void givenSomethingThatIsNotAPlate_whenCreating_thenItIsInvalid(String raw) {
        assertRejectedWith(() -> LicensePlate.of(raw), FIELD, "INVALID_FORMAT");
    }

    @Test
    void givenNullOrBlank_whenCreating_thenItIsRequired() {
        assertRejectedWith(() -> LicensePlate.of(null), FIELD, "REQUIRED");
        assertRejectedWith(() -> LicensePlate.of(" "), FIELD, "REQUIRED");
    }

    @Test
    void givenAPlate_whenShowingItToTheCustomer_thenOnlyTheLastThreeCharactersAppear() {
        assertThat(LicensePlate.of("BRA2E19").lastCharacters()).isEqualTo("E19");
        assertThat(LicensePlate.of("ABC1234").lastCharacters()).isEqualTo("234");
    }

    @Test
    void givenAPlate_whenPrinting_thenItIsNeverShown() {
        assertThat(LicensePlate.of("ABC1234")).hasToString("LicensePlate[redacted]");
    }

    @Test
    void givenTheSamePlateWrittenDifferently_whenComparing_thenTheyAreEqual() {
        assertThat(LicensePlate.of("abc-1234")).isEqualTo(LicensePlate.of("ABC1234"));
    }
}
