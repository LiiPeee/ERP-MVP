package br.com.erpcore.application.support;

import br.com.erpcore.application.vehicle.port.out.VehicleRepository;
import br.com.erpcore.domain.company.CompanyId;
import br.com.erpcore.domain.contact.ContactId;
import br.com.erpcore.domain.vehicle.LicensePlate;
import br.com.erpcore.domain.vehicle.PlateAlreadyRegisteredException;
import br.com.erpcore.domain.vehicle.Vehicle;
import br.com.erpcore.domain.vehicle.VehicleId;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemoryVehicleRepository implements VehicleRepository {

    private final InMemoryUnitOfWork unitOfWork;
    private final Map<VehicleId, Vehicle> storage = new LinkedHashMap<>();

    public InMemoryVehicleRepository(InMemoryUnitOfWork unitOfWork) {
        this.unitOfWork = unitOfWork;
    }

    @Override
    public void save(Vehicle vehicle) {
        unitOfWork.requireActive();
        boolean plateTakenByAnother = storage.values().stream()
                .anyMatch(other -> other.companyId().equals(vehicle.companyId())
                        && other.plate().equals(vehicle.plate()) && !other.id().equals(vehicle.id()));
        if (plateTakenByAnother) {
            throw new PlateAlreadyRegisteredException();
        }
        storage.put(vehicle.id(), vehicle);
    }

    @Override
    public Optional<Vehicle> findById(CompanyId companyId, VehicleId id) {
        unitOfWork.requireActive();
        return Optional.ofNullable(storage.get(id)).filter(vehicle -> vehicle.companyId().equals(companyId));
    }

    @Override
    public Optional<Vehicle> findByPlate(CompanyId companyId, LicensePlate plate) {
        unitOfWork.requireActive();
        return storage.values().stream()
                .filter(vehicle -> vehicle.companyId().equals(companyId) && vehicle.plate().equals(plate))
                .findFirst();
    }

    @Override
    public List<Vehicle> findByContact(CompanyId companyId, ContactId contactId) {
        unitOfWork.requireActive();
        return storage.values().stream()
                .filter(vehicle -> vehicle.companyId().equals(companyId) && vehicle.contactId().equals(contactId))
                .sorted(Comparator.comparing(Vehicle::createdAt))
                .toList();
    }

    public List<Vehicle> all() {
        return List.copyOf(storage.values());
    }
}
