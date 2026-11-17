package br.com.erpcore.domain.identity;

import static br.com.erpcore.domain.shared.DomainValidationAssertions.assertRejectedWith;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PlainPasswordTest {

    private static final String FIELD = "password";
    private static final String SYNTHETIC_PASSWORD = "senha-sintetica-12345";

    @Test
    void givenPasswordWithMinimumLength_whenChoosingANewOne_thenItIsAccepted() {
        String minimum = "a".repeat(PlainPassword.MIN_LENGTH);

        assertThat(PlainPassword.chooseNew(minimum).value()).isEqualTo(minimum);
    }

    @Test
    void givenPasswordWithMaximumLength_whenChoosingANewOne_thenItIsAccepted() {
        String maximum = "a".repeat(PlainPassword.MAX_LENGTH);

        assertThat(PlainPassword.chooseNew(maximum).value()).isEqualTo(maximum);
    }

    @Test
    void givenPasswordShorterThanTheMinimum_whenChoosingANewOne_thenItIsTooShort() {
        String tooShort = "a".repeat(PlainPassword.MIN_LENGTH - 1);

        assertRejectedWith(() -> PlainPassword.chooseNew(tooShort), FIELD, "TOO_SHORT");
    }

    @Test
    void givenPasswordLongerThanTheMaximum_whenChoosingANewOne_thenItIsTooLong() {
        String tooLong = "a".repeat(PlainPassword.MAX_LENGTH + 1);

        assertRejectedWith(() -> PlainPassword.chooseNew(tooLong), FIELD, "TOO_LONG");
    }

    @Test
    void givenNull_whenChoosingANewOne_thenItIsRequired() {
        assertRejectedWith(() -> PlainPassword.chooseNew(null), FIELD, "REQUIRED");
    }

    @Test
    void givenShortPassword_whenReadingALoginAttempt_thenNoCompositionRuleIsApplied() {
        assertThat(PlainPassword.ofLoginAttempt("curta").value()).isEqualTo("curta");
    }

    @Test
    void givenNullOrEmpty_whenReadingALoginAttempt_thenItIsRequired() {
        assertRejectedWith(() -> PlainPassword.ofLoginAttempt(null), FIELD, "REQUIRED");
        assertRejectedWith(() -> PlainPassword.ofLoginAttempt(""), FIELD, "REQUIRED");
    }

    @Test
    void givenPasswordLongerThanTheMaximum_whenReadingALoginAttempt_thenItIsTooLong() {
        String tooLong = "a".repeat(PlainPassword.MAX_LENGTH + 1);

        assertRejectedWith(() -> PlainPassword.ofLoginAttempt(tooLong), FIELD, "TOO_LONG");
    }

    @Test
    void givenAPassword_whenPrintingIt_thenItIsNotExposed() {
        assertThat(PlainPassword.chooseNew(SYNTHETIC_PASSWORD).toString()).doesNotContain("sintetica");
    }
}
