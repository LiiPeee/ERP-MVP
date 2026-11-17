package br.com.erpcore.adapter.persistence.vehicle;

import br.com.erpcore.adapter.persistence.support.FieldColumns;
import br.com.erpcore.adapter.persistence.support.UniqueViolations;
import br.com.erpcore.application.shared.port.out.FieldContext;
import br.com.erpcore.application.vehicle.port.out.VehicleRepository;
import br.com.erpcore.domain.company.CompanyId;
import br.com.erpcore.domain.contact.ContactId;
import br.com.erpcore.domain.vehicle.LicensePlate;
import br.com.erpcore.domain.vehicle.PlateAlreadyRegisteredException;
import br.com.erpcore.domain.vehicle.Vehicle;
import br.com.erpcore.domain.vehicle.VehicleId;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;

/** A placa fica cifrada e ganha um índice cego para busca exata e para a regra de uma placa por oficina. */
public final class JpaVehicleRepository implements VehicleRepository {

    private static final String PLATE_FIELD = "vehicle.plate";
    private static final String PLATE_UNIQUE_CONSTRAINT = "vehicle_plate_unique_per_company";

    private final VehicleJpaRepository jpa;
    private final FieldColumns columns;

    public JpaVehicleRepository(VehicleJpaRepository jpa, FieldColumns columns) {
        this.jpa = jpa;
        this.columns = columns;
    }

    @Override
    public void save(Vehicle vehicle) {
        FieldContext context = new FieldContext(vehicle.companyId(), PLATE_FIELD);
        byte[] plate = columns.reuseOrEncrypt(context,
                jpa.findById(vehicle.id().value()).map(VehicleJpaEntity::getPlateCiphertext).orElse(null), vehicle.plate().value());
        try {
            jpa.saveAndFlush(new VehicleJpaEntity(vehicle.id().value(), vehicle.companyId().value(), vehicle.contactId().value(),
                    plate, columns.blindIndex(context, vehicle.plate().value()), vehicle.brand(), vehicle.model(),
                    vehicle.year(), vehicle.odometerKm().orElse(null), vehicle.createdAt()));
        } catch (DataIntegrityViolationException failure) {
            if (UniqueViolations.isViolationOf(failure, PLATE_UNIQUE_CONSTRAINT)) {
                throw new PlateAlreadyRegisteredException();
            }
            throw failure;
        }
    }

    @Override
    public Optional<Vehicle> findById(CompanyId companyId, VehicleId id) {
        return jpa.findByIdAndCompanyId(id.value(), companyId.value()).map(this::toDomain);
    }

    @Override
    public Optional<Vehicle> findByPlate(CompanyId companyId, LicensePlate plate) {
        String index = columns.blindIndex(new FieldContext(companyId, PLATE_FIELD), plate.value());
        return jpa.findByCompanyIdAndPlateBlindIndex(companyId.value(), index).map(this::toDomain);
    }

    @Override
    public List<Vehicle> findByContact(CompanyId companyId, ContactId contactId) {
        return jpa.findByCompanyIdAndContactIdOrderByCreatedAtAsc(companyId.value(), contactId.value()).stream()
                .map(this::toDomain).toList();
    }

    private Vehicle toDomain(VehicleJpaEntity entity) {
        CompanyId companyId = new CompanyId(entity.getCompanyId());
        LicensePlate plate = new LicensePlate(columns.decrypt(new FieldContext(companyId, PLATE_FIELD), entity.getPlateCiphertext()));
        return new Vehicle(new VehicleId(entity.getId()), companyId, new ContactId(entity.getContactId()), plate,
                entity.getBrand(), entity.getModel(), entity.getYear(), Optional.ofNullable(entity.getOdometerKm()),
                entity.getCreatedAt());
    }
}
