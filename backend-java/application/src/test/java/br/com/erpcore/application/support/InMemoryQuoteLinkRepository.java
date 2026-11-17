package br.com.erpcore.application.support;

import br.com.erpcore.application.quote.port.out.QuoteLinkRepository;
import br.com.erpcore.domain.identity.TokenHash;
import br.com.erpcore.domain.quote.QuoteLink;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemoryQuoteLinkRepository implements QuoteLinkRepository {

    private final InMemoryUnitOfWork unitOfWork;
    private final Map<TokenHash, QuoteLink> storage = new LinkedHashMap<>();

    public InMemoryQuoteLinkRepository(InMemoryUnitOfWork unitOfWork) {
        this.unitOfWork = unitOfWork;
    }

    @Override
    public void save(QuoteLink link) {
        unitOfWork.requireActive();
        storage.put(link.tokenHash(), copy(link));
    }

    @Override
    public Optional<QuoteLink> findByHash(TokenHash hash) {
        unitOfWork.requireActive();
        return Optional.ofNullable(storage.get(hash)).map(InMemoryQuoteLinkRepository::copy);
    }

    public List<QuoteLink> all() {
        return storage.values().stream().map(InMemoryQuoteLinkRepository::copy).toList();
    }

    private static QuoteLink copy(QuoteLink link) {
        return QuoteLink.restore(link.quoteId(), link.companyId(), link.tokenHash(), link.issuedAt(), link.expiresAt(),
                link.decidedAt().orElse(null));
    }
}
