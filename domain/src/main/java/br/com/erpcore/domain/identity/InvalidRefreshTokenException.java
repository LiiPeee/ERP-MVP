package br.com.erpcore.domain.identity;

import br.com.erpcore.domain.shared.DomainException;

public final class InvalidRefreshTokenException extends DomainException {

    public InvalidRefreshTokenException() {
        super("Invalid refresh token");
    }
}
