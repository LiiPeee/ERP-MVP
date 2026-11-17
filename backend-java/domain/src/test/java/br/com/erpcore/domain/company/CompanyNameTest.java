package br.com.erpcore.domain.company;

import static br.com.erpcore.domain.shared.DomainValidationAssertions.assertRejectedWith;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CompanyNameTest {

    private static final String FIELD = "companyName";

    @Test
    void givenNameWithSurroundingSpaces_whenCreating_thenItIsTrimmed() {
        assertThat(CompanyName.of("  Padaria Fictícia Ltda  ").value()).isEqualTo("Padaria Fictícia Ltda");
    }

    @Test
    void givenNullOrBlank_whenCreating_thenItIsRequired() {
        assertRejectedWith(() -> CompanyName.of(null), FIELD, "REQUIRED");
        assertRejectedWith(() -> CompanyName.of("  "), FIELD, "REQUIRED");
    }

    @Test
    void givenNameLongerThanTheLimit_whenCreating_thenItIsTooLong() {
        assertRejectedWith(() -> CompanyName.of("a".repeat(CompanyName.MAX_LENGTH + 1)), FIELD, "TOO_LONG");
    }

    @Test
    void givenNameWithTheMaximumLength_whenCreating_thenItIsAccepted() {
        assertThat(CompanyName.of("a".repeat(CompanyName.MAX_LENGTH)).value()).hasSize(CompanyName.MAX_LENGTH);
    }
}
