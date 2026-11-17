package br.com.erpcore.adapter.persistence.quote;

import br.com.erpcore.application.quote.port.out.QuoteLinkRepository;
import br.com.erpcore.domain.company.CompanyId;
import br.com.erpcore.domain.identity.TokenHash;
import br.com.erpcore.domain.quote.QuoteId;
import br.com.erpcore.domain.quote.QuoteLink;
import java.util.Optional;

public final class JpaQuoteLinkRepository implements QuoteLinkRepository {

    private final QuoteLinkJpaRepository jpa;

    public JpaQuoteLinkRepository(QuoteLinkJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void save(QuoteLink link) {
        jpa.saveAndFlush(new QuoteLinkJpaEntity(link.tokenHash().value(), link.quoteId().value(), link.companyId().value(),
                link.issuedAt(), link.expiresAt(), link.decidedAt().orElse(null)));
    }

    @Override
    public Optional<QuoteLink> findByHash(TokenHash hash) {
        return jpa.findById(hash.value()).map(entity -> QuoteLink.restore(new QuoteId(entity.getQuoteId()),
                new CompanyId(entity.getCompanyId()), new TokenHash(entity.getTokenHash()), entity.getIssuedAt(),
                entity.getExpiresAt(), entity.getDecidedAt()));
    }
}
