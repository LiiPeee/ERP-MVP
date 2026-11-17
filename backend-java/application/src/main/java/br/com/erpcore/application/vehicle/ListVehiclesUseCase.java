package br.com.erpcore.application.vehicle;

import br.com.erpcore.application.contact.port.out.ContactRepository;
import br.com.erpcore.application.identity.OwnerGuard;
import br.com.erpcore.application.shared.port.out.AccessScope;
import br.com.erpcore.application.shared.port.out.UnitOfWork;
import br.com.erpcore.application.vehicle.port.out.VehicleRepository;
import br.com.erpcore.domain.company.CompanyId;
import br.com.erpcore.domain.contact.ContactId;
import br.com.erpcore.domain.contact.ContactNotFoundException;
import br.com.erpcore.domain.identity.UserId;
import java.util.List;

/** Os veículos de um cliente. Cliente de outra oficina é igual a cliente que não existe. */
public final class ListVehiclesUseCase {

    private final UnitOfWork unitOfWork;
    private final ContactRepository contacts;
    private final VehicleRepository vehicles;
    private final OwnerGuard guard;

    public ListVehiclesUseCase(UnitOfWork unitOfWork, ContactRepository contacts, VehicleRepository vehicles,
                               OwnerGuard guard) {
        this.unitOfWork = unitOfWork;
        this.contacts = contacts;
        this.vehicles = vehicles;
        this.guard = guard;
    }

    public List<VehicleView> execute(ListVehiclesQuery query) {
        CompanyId companyId = new CompanyId(query.principal().companyId());
        UserId userId = new UserId(query.principal().userId());
        return unitOfWork.execute(new AccessScope.Tenant(userId, companyId), () -> {
            guard.require(query.principal());
            ContactId contactId = new ContactId(query.contactId());
            contacts.findById(companyId, contactId).orElseThrow(ContactNotFoundException::new);
            return vehicles.findByContact(companyId, contactId).stream().map(VehicleView::from).toList();
        });
    }
}
