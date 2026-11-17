package br.com.erpcore.application.quote;

import br.com.erpcore.application.audit.AuditRecorder;
import br.com.erpcore.application.identity.OwnerGuard;
import br.com.erpcore.application.quote.port.out.PublicQuoteUrls;
import br.com.erpcore.application.quote.port.out.QuoteDelivery;
import br.com.erpcore.application.quote.port.out.QuoteLinkCodec;
import br.com.erpcore.application.quote.port.out.QuoteLinkRepository;
import br.com.erpcore.application.quote.port.out.QuoteRepository;
import br.com.erpcore.application.shared.port.out.AccessScope;
import br.com.erpcore.application.shared.port.out.UnitOfWork;
import br.com.erpcore.domain.audit.AuditEventType;
import br.com.erpcore.domain.company.CompanyId;
import br.com.erpcore.domain.identity.UserId;
import br.com.erpcore.domain.quote.InvalidQuoteTransitionException;
import br.com.erpcore.domain.quote.Quote;
import br.com.erpcore.domain.quote.QuoteId;
import br.com.erpcore.domain.quote.QuoteNotFoundException;
import br.com.erpcore.domain.quote.QuoteStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;

/** Reenvia um orçamento que já foi enviado: novo link, mesma validade, mesmo estado. Os links antigos seguem valendo. */
public final class SendQuoteUseCase {

    private final UnitOfWork unitOfWork;
    private final QuoteRepository quotes;
    private final OwnerGuard guard;
    private final AuditRecorder audit;
    private final Clock clock;
    private final QuoteLinkIssuer issuer;

    public SendQuoteUseCase(UnitOfWork unitOfWork, QuoteRepository quotes, QuoteLinkRepository links,
                            QuoteLinkCodec codec, PublicQuoteUrls urls, QuoteDelivery delivery, OwnerGuard guard,
                            AuditRecorder audit, Clock clock) {
        this.unitOfWork = unitOfWork;
        this.quotes = quotes;
        this.guard = guard;
        this.audit = audit;
        this.clock = clock;
        this.issuer = new QuoteLinkIssuer(links, codec, urls, delivery);
    }

    public QuoteSentResult execute(SendQuoteCommand command) {
        CompanyId companyId = new CompanyId(command.principal().companyId());
        UserId userId = new UserId(command.principal().userId());
        QuoteLinkIssuer.Issued issued = unitOfWork.execute(new AccessScope.Tenant(userId, companyId), () -> {
            guard.require(command.principal());
            Quote quote = quotes.findByIdForUpdate(companyId, new QuoteId(command.quoteId()))
                    .orElseThrow(QuoteNotFoundException::new);
            Instant now = clock.instant();
            QuoteStatus status = quote.statusAt(now);
            if (status != QuoteStatus.SENT) {
                throw new InvalidQuoteTransitionException(status, "send");
            }
            QuoteLinkIssuer.Issued link = issuer.issue(quote, now);
            audit.success(AuditEventType.QUOTE_RESENT, companyId, userId, command.correlationId(),
                    Map.of("quoteId", quote.id().value().toString()));
            return link;
        });
        return issuer.result(issued, issuer.deliver(issued));
    }
}
