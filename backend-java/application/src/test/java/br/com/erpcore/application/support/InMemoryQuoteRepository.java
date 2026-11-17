package br.com.erpcore.application.support;

import br.com.erpcore.application.quote.port.out.QuoteRepository;
import br.com.erpcore.domain.company.CompanyId;
import br.com.erpcore.domain.quote.Quote;
import br.com.erpcore.domain.quote.QuoteId;
import br.com.erpcore.domain.quote.QuoteStatus;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Guarda e devolve cópias, como um banco: alterar o objeto devolvido não altera o guardado até chamar {@code save}. */
public final class InMemoryQuoteRepository implements QuoteRepository {

    private final InMemoryUnitOfWork unitOfWork;
    private final Map<QuoteId, Quote> storage = new LinkedHashMap<>();
    private int lockedReads;

    public InMemoryQuoteRepository(InMemoryUnitOfWork unitOfWork) {
        this.unitOfWork = unitOfWork;
    }

    @Override
    public void save(Quote quote) {
        unitOfWork.requireActive();
        storage.put(quote.id(), copy(quote));
    }

    @Override
    public Optional<Quote> findById(CompanyId companyId, QuoteId id) {
        unitOfWork.requireActive();
        return Optional.ofNullable(storage.get(id)).filter(quote -> quote.companyId().equals(companyId)).map(InMemoryQuoteRepository::copy);
    }

    @Override
    public Optional<Quote> findByIdForUpdate(CompanyId companyId, QuoteId id) {
        lockedReads++;
        return findById(companyId, id);
    }

    @Override
    public List<Quote> findByStatuses(CompanyId companyId, Set<QuoteStatus> statuses, int limit) {
        unitOfWork.requireActive();
        return storage.values().stream()
                .filter(quote -> quote.companyId().equals(companyId) && statuses.contains(quote.status()))
                .sorted(Comparator.comparing(Quote::createdAt).reversed())
                .limit(limit)
                .map(InMemoryQuoteRepository::copy)
                .toList();
    }

    @Override
    public List<Quote> findDueForExpiry(Instant now, int limit) {
        unitOfWork.requireActive();
        return storage.values().stream()
                .filter(quote -> quote.status() == QuoteStatus.SENT && quote.expiresAt().filter(expiry -> !now.isBefore(expiry)).isPresent())
                .sorted(Comparator.comparing((Quote quote) -> quote.expiresAt().orElseThrow()))
                .limit(limit)
                .map(InMemoryQuoteRepository::copy)
                .toList();
    }

    /** Quantas vezes um caso de uso leu com trava, para provar que a decisão do cliente trava a linha. */
    public int lockedReads() {
        return lockedReads;
    }

    private static Quote copy(Quote quote) {
        return Quote.restore(quote.id(), quote.companyId(), quote.contactId(), quote.vehicleId(), quote.validityDays(),
                quote.note(), quote.createdAt(), quote.items(), quote.status(), quote.sentAt().orElse(null),
                quote.expiresAt().orElse(null), quote.decidedAt().orElse(null), quote.decisionReason().orElse(null));
    }
}
