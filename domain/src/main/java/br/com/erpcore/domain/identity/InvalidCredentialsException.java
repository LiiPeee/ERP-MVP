package br.com.erpcore.domain.identity;

import br.com.erpcore.domain.shared.DomainException;

/** Mesma exceção para usuário inexistente, senha errada e conta bloqueada: ninguém descobre qual foi. */
public final class InvalidCredentialsException extends DomainException {

    public InvalidCredentialsException() {
        super("Invalid credentials");
    }
}
