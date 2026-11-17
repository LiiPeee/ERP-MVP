package br.com.erpcore.domain.contact;

import static br.com.erpcore.domain.shared.DomainValidationAssertions.assertRejectedWith;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

class PhoneNumberTest {

    private static final String FIELD = "phone";

    @ParameterizedTest
    @ValueSource(strings = {"(11) 98765-4321", "11987654321", "+55 11 98765-4321", "5511987654321", "+5511987654321", " 11 9 8765 4321 "})
    void givenAMobileNumberWrittenInAnyCommonWay_whenCreating_thenItIsNormalizedToE164(String raw) {
        assertThat(PhoneNumber.of(raw).value()).isEqualTo("+5511987654321");
    }

    @ParameterizedTest
    @ValueSource(strings = {"(11) 3456-7890", "1134567890", "+55 11 3456-7890"})
    void givenALandlineNumber_whenCreating_thenItIsNormalizedToE164(String raw) {
        assertThat(PhoneNumber.of(raw).value()).isEqualTo("+551134567890");
    }

    @Test
    void givenNullOrBlank_whenCreating_thenItIsRequired() {
        assertRejectedWith(() -> PhoneNumber.of(null), FIELD, "REQUIRED");
        assertRejectedWith(() -> PhoneNumber.of("   "), FIELD, "REQUIRED");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "abc", "12345", "119876543", "119876543210", // curto, curto, curto, longo
            "0119876543", "01198765432", // DDD começando em zero
            "11887654321", // celular de 9 dígitos que não começa em 9
            "1117654321", "1107654321", "1197654321", // fixo de 8 dígitos que não começa em 2 a 5
            "+1 415 555 0100", "+351 912 345 678", // outro país
            "11 98765-4321 ramal 12"})
    void givenSomethingThatIsNotABrazilianPhone_whenCreating_thenItIsInvalid(String raw) {
        assertRejectedWith(() -> PhoneNumber.of(raw), FIELD, "INVALID_FORMAT");
    }

    @Test
    void givenAPhone_whenMasking_thenOnlyTheAreaCodeAndTheLastFourDigitsRemain() {
        assertThat(PhoneNumber.of("11987654321").masked()).isEqualTo("(11) *****-4321");
        assertThat(PhoneNumber.of("1134567890").masked()).isEqualTo("(11) ****-7890");
    }

    @Test
    void givenAPhone_whenPrinting_thenTheNumberIsNeverShown() {
        assertThat(PhoneNumber.of("11987654321")).hasToString("PhoneNumber[redacted]");
    }

    @Test
    void givenTheSameNumberWrittenDifferently_whenComparing_thenTheyAreEqual() {
        assertThat(PhoneNumber.of("(11) 98765-4321")).isEqualTo(PhoneNumber.of("+55 11 987654321"));
    }
}
