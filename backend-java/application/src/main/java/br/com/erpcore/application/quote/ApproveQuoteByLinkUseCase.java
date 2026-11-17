package br.com.erpcore.application.quote;

import br.com.erpcore.application.audit.AuditRecorder;
import br.com.erpcore.application.quote.port.out.QuoteLinkCodec;
import br.com.erpcore.application.quote.port.out.QuoteLinkRepository;
import br.com.erpcore.application.quote.port.out.QuoteRepository;
import br.com.erpcore.application.shared.port.out.UnitOfWork;
import br.com.erpcore.domain.audit.AuditEventType;
import java.time.Clock;

public final class ApproveQuoteByLinkUseCase {

    private final QuoteLinkDecision decision;

    public ApproveQuoteByLinkUseCase(UnitOfWork unitOfWork, QuoteRepository quotes, QuoteLinkRepository links,
                                     QuoteLinkCodec codec, AuditRecorder audit, Clock clock) {
        this.decision = new QuoteLinkDecision(unitOfWork, quotes, links, codec, audit, clock);
    }

    public PublicQuoteDecision execute(DecideQuoteByLinkCommand command) {
        return decision.decide(command.token(), AuditEventType.QUOTE_APPROVED_BY_LINK,
                (quote, now) -> quote.approve(now), command.correlationId());
    }
}
