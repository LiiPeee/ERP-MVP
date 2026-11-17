package br.com.erpcore.application.profile;

import br.com.erpcore.application.identity.AuthenticatedPrincipal;

public record GetCompanyProfileQuery(AuthenticatedPrincipal principal) {
}
