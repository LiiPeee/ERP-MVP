package br.com.erpcore.application.vehicle;

import br.com.erpcore.domain.vehicle.Vehicle;
import java.util.UUID;

/** O veículo como a API o devolve: a placa vai mascarada. */
public record VehicleView(UUID id, UUID contactId, String plateMasked, String brand, String model, int year,
                          Integer odometerKm) {

    static VehicleView from(Vehicle vehicle) {
        return new VehicleView(vehicle.id().value(), vehicle.contactId().value(), vehicle.plate().masked(),
                vehicle.brand(), vehicle.model(), vehicle.year(), vehicle.odometerKm().orElse(null));
    }
}
