package br.com.erpcore.application.quote;

import br.com.erpcore.application.audit.AuditRecorder;
import br.com.erpcore.application.profile.port.out.CompanyProfileRepository;
import br.com.erpcore.application.contact.port.out.ContactRepository;
import br.com.erpcore.application.identity.OwnerGuard;
import br.com.erpcore.application.quote.port.out.QuoteRepository;
import br.com.erpcore.application.shared.port.out.AccessScope;
import br.com.erpcore.application.shared.port.out.IdGenerator;
import br.com.erpcore.application.shared.port.out.UnitOfWork;
import br.com.erpcore.application.vehicle.port.out.VehicleRepository;
import br.com.erpcore.domain.audit.AuditEventType;
import br.com.erpcore.domain.company.CompanyId;
import br.com.erpcore.domain.company.CompanyProfile;
import br.com.erpcore.domain.contact.Contact;
import br.com.erpcore.domain.contact.ContactId;
import br.com.erpcore.domain.contact.ContactNotFoundException;
import br.com.erpcore.domain.identity.UserId;
import br.com.erpcore.domain.quote.Quote;
import br.com.erpcore.domain.quote.QuoteId;
import br.com.erpcore.domain.vehicle.Vehicle;
import br.com.erpcore.domain.vehicle.VehicleId;
import br.com.erpcore.domain.vehicle.VehicleNotFoundException;
import java.time.Clock;
import java.util.Map;

/** Cria o rascunho. Quem cria (o dono agora; a IA nas fases seguintes) só entrega texto: o total sai do domínio. */
public final class CreateQuoteDraftUseCase {

    private final UnitOfWork unitOfWork;
    private final ContactRepository contacts;
    private final VehicleRepository vehicles;
    private final QuoteRepository quotes;
    private final CompanyProfileRepository profiles;
    private final OwnerGuard guard;
    private final AuditRecorder audit;
    private final IdGenerator ids;
    private final Clock clock;
    private final QuoteViews views;

    public CreateQuoteDraftUseCase(UnitOfWork unitOfWork, ContactRepository contacts, VehicleRepository vehicles,
                                   QuoteRepository quotes, CompanyProfileRepository profiles, OwnerGuard guard,
                                   AuditRecorder audit, IdGenerator ids, Clock clock) {
        this.unitOfWork = unitOfWork;
        this.contacts = contacts;
        this.vehicles = vehicles;
        this.quotes = quotes;
        this.profiles = profiles;
        this.guard = guard;
        this.audit = audit;
        this.ids = ids;
        this.clock = clock;
        this.views = new QuoteViews(contacts, vehicles, clock);
    }

    public QuoteView execute(CreateQuoteDraftCommand command) {
        CompanyId companyId = new CompanyId(command.principal().companyId());
        UserId userId = new UserId(command.principal().userId());
        return unitOfWork.execute(new AccessScope.Tenant(userId, companyId), () -> {
            guard.require(command.principal());
            Contact contact = contacts.findById(companyId, new ContactId(command.contactId()))
                    .orElseThrow(ContactNotFoundException::new);
            Vehicle vehicle = vehicles.findById(companyId, new VehicleId(command.vehicleId()))
                    .filter(found -> found.contactId().equals(contact.id()))
                    .orElseThrow(VehicleNotFoundException::new);
            int validityDays = command.validityDays() != null ? command.validityDays()
                    : profiles.find(companyId).map(CompanyProfile::defaultValidityDays)
                            .orElse(CompanyProfile.DEFAULT_VALIDITY_DAYS);

            Quote quote = Quote.draft(new QuoteId(ids.newId()), companyId, contact.id(), vehicle.id(), validityDays,
                    command.note(), clock.instant());
            quote.reviseItems(QuoteItemMapper.toDomain(command.items()));
            quotes.save(quote);
            audit.success(AuditEventType.QUOTE_DRAFT_CREATED, companyId, userId, command.correlationId(),
                    Map.of("quoteId", quote.id().value().toString()));
            return views.view(quote);
        });
    }
}
