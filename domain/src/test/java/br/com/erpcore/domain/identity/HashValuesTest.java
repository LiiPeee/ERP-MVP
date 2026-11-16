package br.com.erpcore.domain.identity;

import static br.com.erpcore.domain.shared.DomainValidationAssertions.assertRejectedWith;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HashValuesTest {

    @Test
    void givenBlankPasswordHash_whenCreating_thenItIsRequired() {
        assertRejectedWith(() -> new PasswordHash(" "), "passwordHash", "REQUIRED");
    }

    @Test
    void givenBlankTokenHash_whenCreating_thenItIsRequired() {
        assertRejectedWith(() -> new TokenHash(null), "tokenHash", "REQUIRED");
    }

    @Test
    void givenHashes_whenPrintingThem_thenTheValueIsNotExposed() {
        assertThat(new PasswordHash("$argon2id$fake").toString()).doesNotContain("argon2id");
        assertThat(new TokenHash("abc123fake").toString()).doesNotContain("abc123");
    }
}
