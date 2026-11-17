package br.com.erpcore.application.quote;

import br.com.erpcore.application.audit.AuditRecorder;
import br.com.erpcore.application.quote.port.out.QuoteLinkCodec;
import br.com.erpcore.application.quote.port.out.QuoteLinkRepository;
import br.com.erpcore.application.quote.port.out.QuoteRepository;
import br.com.erpcore.application.shared.port.out.UnitOfWork;
import br.com.erpcore.domain.audit.AuditEventType;
import br.com.erpcore.domain.quote.InvalidQuoteTransitionException;
import br.com.erpcore.domain.quote.Quote;
import br.com.erpcore.domain.quote.QuoteAlreadyDecidedException;
import br.com.erpcore.domain.quote.QuoteLink;
import br.com.erpcore.domain.quote.QuoteLinkUnavailableException;
import br.com.erpcore.domain.quote.QuoteStatus;
import br.com.erpcore.domain.quote.QuoteUnavailableException;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * A decisão do cliente pelo link, comum a aprovar e a recusar. Trava a linha do orçamento, então duas decisões ao
 * mesmo tempo se enfileiram. Repetir a mesma decisão não muda nada (nem gera outro evento); a oposta é recusada.
 */
final class QuoteLinkDecision {

    private final UnitOfWork unitOfWork;
    private final QuoteRepository quotes;
    private final QuoteLinkRepository links;
    private final AuditRecorder audit;
    private final Clock clock;
    private final PublicQuoteAccess access;

    QuoteLinkDecision(UnitOfWork unitOfWork, QuoteRepository quotes, QuoteLinkRepository links, QuoteLinkCodec codec,
                      AuditRecorder audit, Clock clock) {
        this.unitOfWork = unitOfWork;
        this.quotes = quotes;
        this.links = links;
        this.audit = audit;
        this.clock = clock;
        this.access = new PublicQuoteAccess(unitOfWork, links, codec, clock);
    }

    PublicQuoteDecision decide(String token, AuditEventType auditType, BiConsumer<Quote, Instant> decision,
                               String correlationId) {
        PublicQuoteAccess.Located located = access.locate(token);
        return unitOfWork.execute(access.scopeFor(located), () -> {
            Quote quote = quotes.findByIdForUpdate(located.companyId(), located.quoteId())
                    .orElseThrow(QuoteLinkUnavailableException::new);
            QuoteLink link = links.findByHash(located.hash()).orElseThrow(QuoteLinkUnavailableException::new);
            Instant now = clock.instant();
            if (!link.isActiveAt(now)) {
                throw new QuoteLinkUnavailableException();
            }
            QuoteStatus before = quote.status();
            try {
                decision.accept(quote, now);
            } catch (QuoteUnavailableException pastDeadline) {
                throw new QuoteLinkUnavailableException();
            } catch (InvalidQuoteTransitionException wrongState) {
                throw alreadyDecidedOrUnavailable(wrongState.from());
            }
            if (quote.status() != before) {
                link.markDecided(now);
                quotes.save(quote);
                links.save(link);
                audit.systemSuccess(auditType, located.companyId(), correlationId,
                        Map.of("quoteId", quote.id().value().toString()));
            }
            return new PublicQuoteDecision(quote.status().name());
        });
    }

    private static RuntimeException alreadyDecidedOrUnavailable(QuoteStatus from) {
        return from == QuoteStatus.APPROVED || from == QuoteStatus.REJECTED
                ? new QuoteAlreadyDecidedException() : new QuoteLinkUnavailableException();
    }
}
