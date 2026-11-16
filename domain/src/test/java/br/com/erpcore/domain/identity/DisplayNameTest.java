package br.com.erpcore.domain.identity;

import static br.com.erpcore.domain.shared.DomainValidationAssertions.assertRejectedWith;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DisplayNameTest {

    private static final String FIELD = "displayName";

    @Test
    void givenNameWithSurroundingSpaces_whenCreating_thenItIsTrimmed() {
        assertThat(DisplayName.of("  Ana Fictícia ").value()).isEqualTo("Ana Fictícia");
    }

    @Test
    void givenNullOrBlank_whenCreating_thenItIsRequired() {
        assertRejectedWith(() -> DisplayName.of(null), FIELD, "REQUIRED");
        assertRejectedWith(() -> DisplayName.of(""), FIELD, "REQUIRED");
    }

    @Test
    void givenNameLongerThanTheLimit_whenCreating_thenItIsTooLong() {
        assertRejectedWith(() -> DisplayName.of("a".repeat(DisplayName.MAX_LENGTH + 1)), FIELD, "TOO_LONG");
    }
}
