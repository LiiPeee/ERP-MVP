package br.com.erpcore.application.quote;

import br.com.erpcore.application.audit.AuditRecorder;
import br.com.erpcore.application.contact.port.out.ContactRepository;
import br.com.erpcore.application.identity.OwnerGuard;
import br.com.erpcore.application.quote.port.out.QuoteRepository;
import br.com.erpcore.application.shared.port.out.AccessScope;
import br.com.erpcore.application.shared.port.out.UnitOfWork;
import br.com.erpcore.application.vehicle.port.out.VehicleRepository;
import br.com.erpcore.domain.audit.AuditEventType;
import br.com.erpcore.domain.company.CompanyId;
import br.com.erpcore.domain.identity.UserId;
import br.com.erpcore.domain.quote.Quote;
import br.com.erpcore.domain.quote.QuoteId;
import br.com.erpcore.domain.quote.QuoteNotFoundException;
import java.time.Clock;
import java.util.Map;

public final class ReviseQuoteItemsUseCase {

    private final UnitOfWork unitOfWork;
    private final QuoteRepository quotes;
    private final OwnerGuard guard;
    private final AuditRecorder audit;
    private final QuoteViews views;

    public ReviseQuoteItemsUseCase(UnitOfWork unitOfWork, ContactRepository contacts, VehicleRepository vehicles,
                                   QuoteRepository quotes, OwnerGuard guard, AuditRecorder audit, Clock clock) {
        this.unitOfWork = unitOfWork;
        this.quotes = quotes;
        this.guard = guard;
        this.audit = audit;
        this.views = new QuoteViews(contacts, vehicles, clock);
    }

    public QuoteView execute(ReviseQuoteItemsCommand command) {
        CompanyId companyId = new CompanyId(command.principal().companyId());
        UserId userId = new UserId(command.principal().userId());
        return unitOfWork.execute(new AccessScope.Tenant(userId, companyId), () -> {
            guard.require(command.principal());
            Quote quote = quotes.findByIdForUpdate(companyId, new QuoteId(command.quoteId()))
                    .orElseThrow(QuoteNotFoundException::new);
            quote.reviseItems(QuoteItemMapper.toDomain(command.items()));
            quotes.save(quote);
            audit.success(AuditEventType.QUOTE_ITEMS_REVISED, companyId, userId, command.correlationId(),
                    Map.of("quoteId", quote.id().value().toString()));
            return views.view(quote);
        });
    }
}
