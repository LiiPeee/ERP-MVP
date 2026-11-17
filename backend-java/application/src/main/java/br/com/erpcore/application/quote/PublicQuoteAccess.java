package br.com.erpcore.application.quote;

import br.com.erpcore.application.quote.port.out.QuoteLinkCodec;
import br.com.erpcore.application.quote.port.out.QuoteLinkRepository;
import br.com.erpcore.application.shared.port.out.AccessScope;
import br.com.erpcore.application.shared.port.out.UnitOfWork;
import br.com.erpcore.domain.company.CompanyId;
import br.com.erpcore.domain.identity.TokenHash;
import br.com.erpcore.domain.quote.QuoteId;
import br.com.erpcore.domain.quote.QuoteLink;
import br.com.erpcore.domain.quote.QuoteLinkUnavailableException;
import java.time.Clock;

/**
 * Do token ao orçamento, em duas transações: esta, sob o escopo que só enxerga o link com aquele hash, descobre a
 * empresa e o orçamento; a seguinte roda presa a esse único orçamento. Token ausente, desconhecido ou vencido dão
 * sempre a mesma resposta.
 */
final class PublicQuoteAccess {

    private final UnitOfWork unitOfWork;
    private final QuoteLinkRepository links;
    private final QuoteLinkCodec codec;
    private final Clock clock;

    PublicQuoteAccess(UnitOfWork unitOfWork, QuoteLinkRepository links, QuoteLinkCodec codec, Clock clock) {
        this.unitOfWork = unitOfWork;
        this.links = links;
        this.codec = codec;
        this.clock = clock;
    }

    Located locate(String token) {
        if (token == null || token.isBlank()) {
            throw new QuoteLinkUnavailableException();
        }
        TokenHash hash = new TokenHash(codec.hash(token));
        QuoteLink link = unitOfWork.execute(new AccessScope.QuoteLinkLookup(hash), () -> links.findByHash(hash))
                .filter(found -> found.isActiveAt(clock.instant()))
                .orElseThrow(QuoteLinkUnavailableException::new);
        return new Located(hash, link.companyId(), link.quoteId());
    }

    AccessScope.QuoteDecision scopeFor(Located located) {
        return new AccessScope.QuoteDecision(located.companyId(), located.quoteId());
    }

    record Located(TokenHash hash, CompanyId companyId, QuoteId quoteId) {
    }
}
