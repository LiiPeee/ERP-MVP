package br.com.erpcore.application.contact;

import br.com.erpcore.application.identity.AuthenticatedPrincipal;

/** {@code nameFragment} nulo ou em branco lista os mais recentes. {@code limit} fora do intervalo vira o padrão ou o teto. */
public record ListContactsQuery(AuthenticatedPrincipal principal, String nameFragment, int limit) {
}
