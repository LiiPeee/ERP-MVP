package br.com.erpcore.application.contact;

import br.com.erpcore.application.audit.AuditRecorder;
import br.com.erpcore.application.contact.port.out.ContactRepository;
import br.com.erpcore.application.identity.OwnerGuard;
import br.com.erpcore.application.shared.port.out.AccessScope;
import br.com.erpcore.application.shared.port.out.IdGenerator;
import br.com.erpcore.application.shared.port.out.UnitOfWork;
import br.com.erpcore.domain.audit.AuditEventType;
import br.com.erpcore.domain.company.CompanyId;
import br.com.erpcore.domain.contact.Contact;
import br.com.erpcore.domain.contact.ContactId;
import br.com.erpcore.domain.contact.ContactName;
import br.com.erpcore.domain.contact.PhoneNumber;
import br.com.erpcore.domain.identity.Email;
import br.com.erpcore.domain.identity.UserId;
import java.time.Clock;
import java.util.Map;

public final class CreateContactUseCase {

    private final UnitOfWork unitOfWork;
    private final ContactRepository contacts;
    private final OwnerGuard guard;
    private final AuditRecorder audit;
    private final IdGenerator ids;
    private final Clock clock;

    public CreateContactUseCase(UnitOfWork unitOfWork, ContactRepository contacts, OwnerGuard guard,
                                AuditRecorder audit, IdGenerator ids, Clock clock) {
        this.unitOfWork = unitOfWork;
        this.contacts = contacts;
        this.guard = guard;
        this.audit = audit;
        this.ids = ids;
        this.clock = clock;
    }

    public ContactView execute(CreateContactCommand command) {
        CompanyId companyId = new CompanyId(command.principal().companyId());
        UserId userId = new UserId(command.principal().userId());
        return unitOfWork.execute(new AccessScope.Tenant(userId, companyId), () -> {
            guard.require(command.principal());
            ContactName name = ContactName.of(command.name());
            PhoneNumber phone = PhoneNumber.of(command.phone());
            Email email = command.email() == null || command.email().isBlank() ? null : Email.of(command.email());
            Contact contact = Contact.create(new ContactId(ids.newId()), companyId, name, phone, email, clock.instant());
            contacts.save(contact);
            audit.success(AuditEventType.CONTACT_CREATED, companyId, userId, command.correlationId(),
                    Map.of("contactId", contact.id().value().toString()));
            return ContactView.from(contact);
        });
    }
}
