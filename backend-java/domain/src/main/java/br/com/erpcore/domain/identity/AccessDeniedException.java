package br.com.erpcore.domain.identity;

import br.com.erpcore.domain.shared.DomainException;

public final class AccessDeniedException extends DomainException {

    public AccessDeniedException() {
        super("Access denied");
    }
}
