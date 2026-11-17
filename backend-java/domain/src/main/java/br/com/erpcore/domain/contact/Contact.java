package br.com.erpcore.domain.contact;

import br.com.erpcore.domain.company.CompanyId;
import br.com.erpcore.domain.identity.Email;
import java.time.Instant;
import java.util.Optional;

/** Cliente da oficina. Nome, telefone e e-mail são dados pessoais de terceiro. */
public record Contact(ContactId id, CompanyId companyId, ContactName name, PhoneNumber phone, Optional<Email> email,
                      Instant createdAt) {

    public static Contact create(ContactId id, CompanyId companyId, ContactName name, PhoneNumber phone, Email email,
                                 Instant now) {
        return new Contact(id, companyId, name, phone, Optional.ofNullable(email), now);
    }
}
