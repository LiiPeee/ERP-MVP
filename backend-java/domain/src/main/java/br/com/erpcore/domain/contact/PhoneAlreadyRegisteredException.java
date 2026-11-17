package br.com.erpcore.domain.contact;

import br.com.erpcore.domain.shared.DomainException;

/** Já existe um cliente com este telefone na oficina. Só quem está autenticado na oficina chega a ver isto. */
public final class PhoneAlreadyRegisteredException extends DomainException {

    public PhoneAlreadyRegisteredException() {
        super("Phone already registered");
    }
}
