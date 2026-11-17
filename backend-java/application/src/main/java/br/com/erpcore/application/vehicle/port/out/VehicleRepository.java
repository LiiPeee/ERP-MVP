package br.com.erpcore.application.vehicle.port.out;

import br.com.erpcore.domain.company.CompanyId;
import br.com.erpcore.domain.contact.ContactId;
import br.com.erpcore.domain.vehicle.LicensePlate;
import br.com.erpcore.domain.vehicle.Vehicle;
import br.com.erpcore.domain.vehicle.VehicleId;
import java.util.List;
import java.util.Optional;

public interface VehicleRepository {

    /**
     * Insere ou atualiza. Duas placas iguais na mesma oficina não são aceitas; a tentativa lança
     * {@link br.com.erpcore.domain.vehicle.PlateAlreadyRegisteredException}.
     */
    void save(Vehicle vehicle);

    Optional<Vehicle> findById(CompanyId companyId, VehicleId id);

    /** Busca exata pela placa (no banco, por índice cego). */
    Optional<Vehicle> findByPlate(CompanyId companyId, LicensePlate plate);

    /** Os veículos de um cliente, do mais antigo ao mais novo. */
    List<Vehicle> findByContact(CompanyId companyId, ContactId contactId);
}
