package br.com.erpcore.application.contact;

import br.com.erpcore.application.identity.AuthenticatedPrincipal;

/** O e-mail é opcional (nulo ou em branco = sem e-mail). */
public record CreateContactCommand(AuthenticatedPrincipal principal, String name, String phone, String email,
                                   String correlationId) {

    @Override
    public String toString() {
        return "CreateContactCommand[redacted]";
    }
}
