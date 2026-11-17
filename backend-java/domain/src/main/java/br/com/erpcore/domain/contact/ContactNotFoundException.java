package br.com.erpcore.domain.contact;

import br.com.erpcore.domain.shared.DomainException;

public final class ContactNotFoundException extends DomainException {

    public ContactNotFoundException() {
        super("Contact not found");
    }
}
