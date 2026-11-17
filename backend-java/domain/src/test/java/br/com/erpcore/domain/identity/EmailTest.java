package br.com.erpcore.domain.identity;

import static br.com.erpcore.domain.shared.DomainValidationAssertions.assertRejectedWith;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EmailTest {

    private static final String FIELD = "email";

    @Test
    void givenAddressWithSpacesAndUpperCase_whenCreating_thenItIsTrimmedAndLowerCased() {
        Email email = Email.of("  Dona.Fake@Exemplo.TEST ");

        assertThat(email.value()).isEqualTo("dona.fake@exemplo.test");
    }

    @Test
    void givenTwoAddressesDifferingOnlyByCase_whenComparing_thenTheyAreEqual() {
        assertThat(Email.of("ANA@exemplo.test")).isEqualTo(Email.of("ana@EXEMPLO.test"));
    }

    @Test
    void givenNull_whenCreating_thenItIsRequired() {
        assertRejectedWith(() -> Email.of(null), FIELD, "REQUIRED");
    }

    @Test
    void givenBlank_whenCreating_thenItIsRequired() {
        assertRejectedWith(() -> Email.of("   "), FIELD, "REQUIRED");
    }

    @ParameterizedTest
    @ValueSource(strings = {"sem-arroba.test", "@sem-usuario.test", "sem-dominio@", "sem@ponto", "dois@@exemplo.test", "com espaco@exemplo.test"})
    void givenMalformedAddress_whenCreating_thenFormatIsInvalid(String malformed) {
        assertRejectedWith(() -> Email.of(malformed), FIELD, "INVALID_FORMAT");
    }

    @Test
    void givenAddressLongerThanTheLimit_whenCreating_thenItIsTooLong() {
        String tooLong = "a".repeat(Email.MAX_LENGTH) + "@exemplo.test";

        assertRejectedWith(() -> Email.of(tooLong), FIELD, "TOO_LONG");
    }

    @Test
    void givenAnEmail_whenMasking_thenOnlyTheFirstLetterAndTheDomainRemain() {
        assertThat(Email.of("ana.ficticia@exemplo.test").masked()).isEqualTo("a***@exemplo.test");
        assertThat(Email.of("x@exemplo.test").masked()).isEqualTo("x***@exemplo.test");
    }

    @Test
    void givenAnEmail_whenPrintingIt_thenTheAddressIsNotExposed() {
        assertThat(Email.of("ana@exemplo.test").toString()).doesNotContain("ana").doesNotContain("exemplo");
    }
}
