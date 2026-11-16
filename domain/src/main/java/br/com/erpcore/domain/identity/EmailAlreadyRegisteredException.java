package br.com.erpcore.domain.identity;

import br.com.erpcore.domain.shared.DomainException;

public final class EmailAlreadyRegisteredException extends DomainException {

    public EmailAlreadyRegisteredException() {
        super("Email already registered");
    }
}
