package br.com.erpcore.application.quote;

import br.com.erpcore.application.contact.port.out.ContactRepository;
import br.com.erpcore.application.identity.OwnerGuard;
import br.com.erpcore.application.quote.port.out.QuoteRepository;
import br.com.erpcore.application.shared.port.out.AccessScope;
import br.com.erpcore.application.shared.port.out.UnitOfWork;
import br.com.erpcore.application.vehicle.port.out.VehicleRepository;
import br.com.erpcore.domain.company.CompanyId;
import br.com.erpcore.domain.identity.UserId;
import br.com.erpcore.domain.quote.QuoteId;
import br.com.erpcore.domain.quote.QuoteNotFoundException;
import java.time.Clock;

public final class GetQuoteUseCase {

    private final UnitOfWork unitOfWork;
    private final QuoteRepository quotes;
    private final OwnerGuard guard;
    private final QuoteViews views;

    public GetQuoteUseCase(UnitOfWork unitOfWork, ContactRepository contacts, VehicleRepository vehicles,
                           QuoteRepository quotes, OwnerGuard guard, Clock clock) {
        this.unitOfWork = unitOfWork;
        this.quotes = quotes;
        this.guard = guard;
        this.views = new QuoteViews(contacts, vehicles, clock);
    }

    public QuoteView execute(GetQuoteQuery query) {
        CompanyId companyId = new CompanyId(query.principal().companyId());
        UserId userId = new UserId(query.principal().userId());
        return unitOfWork.execute(new AccessScope.Tenant(userId, companyId), () -> {
            guard.require(query.principal());
            return views.view(quotes.findById(companyId, new QuoteId(query.quoteId()))
                    .orElseThrow(QuoteNotFoundException::new));
        });
    }
}
