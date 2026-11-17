package br.com.erpcore.application.vehicle;

import br.com.erpcore.application.audit.AuditRecorder;
import br.com.erpcore.application.contact.port.out.ContactRepository;
import br.com.erpcore.application.identity.OwnerGuard;
import br.com.erpcore.application.shared.port.out.AccessScope;
import br.com.erpcore.application.shared.port.out.IdGenerator;
import br.com.erpcore.application.shared.port.out.UnitOfWork;
import br.com.erpcore.application.vehicle.port.out.VehicleRepository;
import br.com.erpcore.domain.audit.AuditEventType;
import br.com.erpcore.domain.company.CompanyId;
import br.com.erpcore.domain.contact.ContactId;
import br.com.erpcore.domain.contact.ContactNotFoundException;
import br.com.erpcore.domain.identity.UserId;
import br.com.erpcore.domain.vehicle.LicensePlate;
import br.com.erpcore.domain.vehicle.Vehicle;
import br.com.erpcore.domain.vehicle.VehicleId;
import java.time.Clock;
import java.util.Map;

public final class RegisterVehicleUseCase {

    private final UnitOfWork unitOfWork;
    private final ContactRepository contacts;
    private final VehicleRepository vehicles;
    private final OwnerGuard guard;
    private final AuditRecorder audit;
    private final IdGenerator ids;
    private final Clock clock;

    public RegisterVehicleUseCase(UnitOfWork unitOfWork, ContactRepository contacts, VehicleRepository vehicles,
                                  OwnerGuard guard, AuditRecorder audit, IdGenerator ids, Clock clock) {
        this.unitOfWork = unitOfWork;
        this.contacts = contacts;
        this.vehicles = vehicles;
        this.guard = guard;
        this.audit = audit;
        this.ids = ids;
        this.clock = clock;
    }

    public VehicleView execute(RegisterVehicleCommand command) {
        CompanyId companyId = new CompanyId(command.principal().companyId());
        UserId userId = new UserId(command.principal().userId());
        return unitOfWork.execute(new AccessScope.Tenant(userId, companyId), () -> {
            guard.require(command.principal());
            ContactId contactId = new ContactId(command.contactId());
            contacts.findById(companyId, contactId).orElseThrow(ContactNotFoundException::new);
            Vehicle vehicle = Vehicle.register(new VehicleId(ids.newId()), companyId, contactId,
                    LicensePlate.of(command.plate()), command.brand(), command.model(), command.year(),
                    command.odometerKm(), clock.instant());
            vehicles.save(vehicle);
            audit.success(AuditEventType.VEHICLE_REGISTERED, companyId, userId, command.correlationId(),
                    Map.of("vehicleId", vehicle.id().value().toString()));
            return VehicleView.from(vehicle);
        });
    }
}
