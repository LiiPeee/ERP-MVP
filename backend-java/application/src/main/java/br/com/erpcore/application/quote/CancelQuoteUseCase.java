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
import br.com.erpcore.domain.quote.QuoteStatus;
import java.time.Clock;
import java.util.Map;

/** Cancela um orçamento ainda não decidido. Cancelar de novo não muda nada e não gera outro evento de auditoria. */
public final class CancelQuoteUseCase {

    private final UnitOfWork unitOfWork;
    private final QuoteRepository quotes;
    private final OwnerGuard guard;
    private final AuditRecorder audit;
    private final Clock clock;
    private final QuoteViews views;

    public CancelQuoteUseCase(UnitOfWork unitOfWork, ContactRepository contacts, VehicleRepository vehicles,
                              QuoteRepository quotes, OwnerGuard guard, AuditRecorder audit, Clock clock) {
        this.unitOfWork = unitOfWork;
        this.quotes = quotes;
        this.guard = guard;
        this.audit = audit;
        this.clock = clock;
        this.views = new QuoteViews(contacts, vehicles, clock);
    }

    public QuoteView execute(CancelQuoteCommand command) {
        CompanyId companyId = new CompanyId(command.principal().companyId());
        UserId userId = new UserId(command.principal().userId());
        return unitOfWork.execute(new AccessScope.Tenant(userId, companyId), () -> {
            guard.require(command.principal());
            Quote quote = quotes.findByIdForUpdate(companyId, new QuoteId(command.quoteId()))
                    .orElseThrow(QuoteNotFoundException::new);
            QuoteStatus before = quote.status();
            quote.cancel(clock.instant());
            if (quote.status() != before) {
                quotes.save(quote);
                audit.success(AuditEventType.QUOTE_CANCELLED, companyId, userId, command.correlationId(),
                        Map.of("quoteId", quote.id().value().toString()));
            }
            return views.view(quote);
        });
    }
}
