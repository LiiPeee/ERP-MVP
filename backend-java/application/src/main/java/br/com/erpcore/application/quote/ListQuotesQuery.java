package br.com.erpcore.application.quote;

import br.com.erpcore.application.identity.AuthenticatedPrincipal;

public record ListQuotesQuery(AuthenticatedPrincipal principal) {
}
