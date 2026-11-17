package br.com.erpcore.domain.vehicle;

import br.com.erpcore.domain.shared.DomainException;

/** Já existe um veículo com esta placa na oficina. Troca de dono fica fora do escopo desta fase. */
public final class PlateAlreadyRegisteredException extends DomainException {

    public PlateAlreadyRegisteredException() {
        super("Plate already registered");
    }
}
