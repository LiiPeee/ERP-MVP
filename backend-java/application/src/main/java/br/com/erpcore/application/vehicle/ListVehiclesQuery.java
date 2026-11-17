package br.com.erpcore.application.vehicle;

import br.com.erpcore.application.identity.AuthenticatedPrincipal;
import java.util.UUID;

public record ListVehiclesQuery(AuthenticatedPrincipal principal, UUID contactId) {
}
