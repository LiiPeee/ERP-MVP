package br.com.erpcore.adapter.persistence.quote;

import br.com.erpcore.application.quote.port.out.QuoteRepository;
import br.com.erpcore.domain.company.CompanyId;
import br.com.erpcore.domain.contact.ContactId;
import br.com.erpcore.domain.quote.Quote;
import br.com.erpcore.domain.quote.QuoteId;
import br.com.erpcore.domain.quote.QuoteItem;
import br.com.erpcore.domain.quote.QuoteItemKind;
import br.com.erpcore.domain.quote.QuoteStatus;
import br.com.erpcore.domain.shared.Money;
import br.com.erpcore.domain.shared.Quantity;
import br.com.erpcore.domain.vehicle.VehicleId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;

/**
 * Os itens só são inseridos. Se os itens do orçamento mudaram, grava-se um conjunto novo (revisão + 1) e o orçamento
 * passa a apontar para ele; o conjunto antigo fica como histórico. O total nunca é guardado: sai dos itens.
 */
public final class JpaQuoteRepository implements QuoteRepository {

    private static final int FIRST_REVISION = 1;

    private final QuoteJpaRepository quotes;
    private final QuoteItemJpaRepository items;

    public JpaQuoteRepository(QuoteJpaRepository quotes, QuoteItemJpaRepository items) {
        this.quotes = quotes;
        this.items = items;
    }

    @Override
    public void save(Quote quote) {
        Optional<QuoteJpaEntity> stored = quotes.findById(quote.id().value());
        if (stored.isEmpty()) {
            quotes.saveAndFlush(new QuoteJpaEntity(quote.id().value(), quote.companyId().value(), quote.contactId().value(),
                    quote.vehicleId().value(), quote.status().name(), quote.validityDays(), quote.note(), FIRST_REVISION,
                    quote.createdAt(), quote.sentAt().orElse(null), quote.expiresAt().orElse(null),
                    quote.decidedAt().orElse(null), quote.decisionReason().orElse(null)));
            insertItems(quote, FIRST_REVISION);
            return;
        }
        QuoteJpaEntity entity = stored.get();
        int revision = entity.getItemsRevision();
        if (!readItems(entity).equals(quote.items())) {
            revision++;
            insertItems(quote, revision);
        }
        entity.update(quote.status().name(), revision, quote.sentAt().orElse(null), quote.expiresAt().orElse(null),
                quote.decidedAt().orElse(null), quote.decisionReason().orElse(null));
        quotes.saveAndFlush(entity);
    }

    @Override
    public Optional<Quote> findById(CompanyId companyId, QuoteId id) {
        return quotes.findByIdAndCompanyId(id.value(), companyId.value()).map(this::toDomain);
    }

    @Override
    public Optional<Quote> findByIdForUpdate(CompanyId companyId, QuoteId id) {
        return quotes.lockByIdAndCompanyId(id.value(), companyId.value()).map(this::toDomain);
    }

    @Override
    public List<Quote> findByStatuses(CompanyId companyId, Set<QuoteStatus> statuses, int limit) {
        if (statuses.isEmpty()) {
            return List.of();
        }
        return quotes.findByStatuses(companyId.value(), statuses.stream().map(Enum::name).toList(), PageRequest.of(0, limit))
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<Quote> findDueForExpiry(Instant now, int limit) {
        return quotes.findDueForExpiry(now, PageRequest.of(0, limit)).stream().map(this::toDomain).toList();
    }

    private void insertItems(Quote quote, int revision) {
        List<QuoteItem> lines = quote.items();
        for (int position = 0; position < lines.size(); position++) {
            QuoteItem line = lines.get(position);
            items.save(new QuoteItemJpaEntity(quote.id().value(), revision, position, quote.companyId().value(),
                    line.description(), line.kind().name(), line.quantity().value(), line.unitPrice().amount()));
        }
        items.flush();
    }

    private List<QuoteItem> readItems(QuoteJpaEntity entity) {
        return items.findRevision(entity.getId(), entity.getItemsRevision()).stream()
                .map(row -> QuoteItem.of(row.getDescription(), QuoteItemKind.valueOf(row.getKind()),
                        new Quantity(row.getQuantity()), new Money(row.getUnitPrice())))
                .toList();
    }

    private Quote toDomain(QuoteJpaEntity entity) {
        UUID id = entity.getId();
        return Quote.restore(new QuoteId(id), new CompanyId(entity.getCompanyId()), new ContactId(entity.getContactId()),
                new VehicleId(entity.getVehicleId()), entity.getValidityDays(), entity.getNote(), entity.getCreatedAt(),
                readItems(entity), QuoteStatus.valueOf(entity.getStatus()), entity.getSentAt(), entity.getExpiresAt(),
                entity.getDecidedAt(), entity.getDecisionReason());
    }
}
