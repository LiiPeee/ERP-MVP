package br.com.erpcore.application.vehicle;

import br.com.erpcore.application.identity.AuthenticatedPrincipal;
import java.util.UUID;

/** A quilometragem é opcional (nula = não informada). */
public record RegisterVehicleCommand(AuthenticatedPrincipal principal, UUID contactId, String plate, String brand,
                                     String model, int year, Integer odometerKm, String correlationId) {

    @Override
    public String toString() {
        return "RegisterVehicleCommand[redacted]";
    }
}
