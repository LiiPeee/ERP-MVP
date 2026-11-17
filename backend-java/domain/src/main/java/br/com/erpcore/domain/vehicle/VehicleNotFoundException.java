package br.com.erpcore.domain.vehicle;

import br.com.erpcore.domain.shared.DomainException;

public final class VehicleNotFoundException extends DomainException {

    public VehicleNotFoundException() {
        super("Vehicle not found");
    }
}
