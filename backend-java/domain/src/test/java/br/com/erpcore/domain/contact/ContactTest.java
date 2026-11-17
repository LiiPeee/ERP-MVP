package br.com.erpcore.domain.contact;

import static br.com.erpcore.domain.shared.DomainValidationAssertions.assertRejectedWith;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.erpcore.domain.company.CompanyId;
import br.com.erpcore.domain.identity.Email;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ContactTest {

    private static final Instant NOW = Instant.parse("2026-07-01T12:00:00Z");
    private static final CompanyId COMPANY = new CompanyId(UUID.fromString("00000000-0000-0000-0000-0000000000c1"));

    @Test
    void givenNameWithSurroundingSpaces_whenCreatingTheName_thenItIsTrimmed() {
        assertThat(ContactName.of("  Cliente Fictício  ").value()).isEqualTo("Cliente Fictício");
    }

    @Test
    void givenBlankOrTooLongName_whenCreatingTheName_thenItIsRejected() {
        assertRejectedWith(() -> ContactName.of(" "), "contactName", "REQUIRED");
        assertRejectedWith(() -> ContactName.of(null), "contactName", "REQUIRED");
        assertRejectedWith(() -> ContactName.of("a".repeat(ContactName.MAX_LENGTH + 1)), "contactName", "TOO_LONG");
    }

    @Test
    void givenNameAndPhone_whenCreatingAContact_thenTheEmailIsOptional() {
        Contact contact = Contact.create(new ContactId(UUID.randomUUID()), COMPANY, ContactName.of("Cliente Fictício"),
                PhoneNumber.of("11987654321"), null, NOW);

        assertThat(contact.email()).isEmpty();
        assertThat(contact.companyId()).isEqualTo(COMPANY);
        assertThat(contact.createdAt()).isEqualTo(NOW);
    }

    @Test
    void givenAnEmail_whenCreatingAContact_thenItIsKept() {
        Email email = Email.of("cliente@exemplo.test");

        Contact contact = Contact.create(new ContactId(UUID.randomUUID()), COMPANY, ContactName.of("Cliente Fictício"),
                PhoneNumber.of("11987654321"), email, NOW);

        assertThat(contact.email()).isEqualTo(Optional.of(email));
    }

    @Test
    void givenAContact_whenPrinting_thenNoPersonalDataAppears() {
        Contact contact = Contact.create(new ContactId(UUID.randomUUID()), COMPANY, ContactName.of("Cliente Fictício"),
                PhoneNumber.of("11987654321"), Email.of("cliente@exemplo.test"), NOW);

        assertThat(contact.toString()).doesNotContain("Cliente Fictício", "987654321", "cliente@exemplo.test");
    }
}
