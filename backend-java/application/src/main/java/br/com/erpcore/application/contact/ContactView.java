package br.com.erpcore.application.contact;

import br.com.erpcore.domain.contact.Contact;
import br.com.erpcore.domain.contact.PhoneNumber;
import br.com.erpcore.domain.identity.Email;
import java.time.Instant;
import java.util.UUID;

/** O cliente como a API o devolve: telefone e e-mail sempre mascarados. */
public record ContactView(UUID id, String name, String phoneMasked, String emailMasked, Instant createdAt) {

    static ContactView from(Contact contact) {
        return new ContactView(contact.id().value(), contact.name().value(), contact.phone().masked(),
                contact.email().map(Email::masked).orElse(null), contact.createdAt());
    }

    @Override
    public String toString() {
        return "ContactView[id=" + id + "]";
    }
}
