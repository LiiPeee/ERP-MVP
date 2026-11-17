package br.com.erpcore.application.company;

import br.com.erpcore.application.identity.AuthenticatedPrincipal;

public record GetCompanyProfileQuery(AuthenticatedPrincipal principal) {
}
