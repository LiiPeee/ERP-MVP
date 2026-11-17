package br.com.erpcore.domain.vehicle;

import java.util.Objects;
import java.util.UUID;

public record VehicleId(UUID value) {

    public VehicleId {
        Objects.requireNonNull(value, "value");
    }
}
