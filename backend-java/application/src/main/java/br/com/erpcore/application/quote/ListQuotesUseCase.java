package br.com.erpcore.application.quote;

import br.com.erpcore.application.contact.port.out.ContactRepository;
import br.com.erpcore.application.identity.OwnerGuard;
import br.com.erpcore.application.quote.port.out.QuoteRepository;
import br.com.erpcore.application.shared.port.out.AccessScope;
import br.com.erpcore.application.shared.port.out.UnitOfWork;
import br.com.erpcore.application.vehicle.port.out.VehicleRepository;
import br.com.erpcore.domain.company.CompanyId;
import br.com.erpcore.domain.identity.UserId;
import br.com.erpcore.domain.quote.Quote;
import br.com.erpcore.domain.quote.QuoteStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * A fila do dono: primeiro o que precisa dele, depois o que espera o cliente, depois o que fechou. Orçamento enviado
 * fora do prazo já conta como fechado (vencido), mesmo antes de a tarefa agendada rodar.
 * <p>
 * Cada bloco tem teto (contra raspagem e para a tela). A montagem de cada linha lê o cliente e o veículo; com o teto
 * isso é limitado, e uma consulta em lote fica para quando a medição pedir.
 */
public final class ListQuotesUseCase {

    public static final int OPEN_LIMIT = 50;
    public static final int CLOSED_LIMIT = 30;

    private static final Set<QuoteStatus> NEEDS_OWNER = Set.of(QuoteStatus.DRAFT, QuoteStatus.AWAITING_OWNER_CONFIRMATION);
    private static final Set<QuoteStatus> SENT = Set.of(QuoteStatus.SENT);
    private static final Set<QuoteStatus> CLOSED = Set.of(QuoteStatus.APPROVED, QuoteStatus.REJECTED,
            QuoteStatus.EXPIRED, QuoteStatus.CANCELLED);

    private final UnitOfWork unitOfWork;
    private final QuoteRepository quotes;
    private final OwnerGuard guard;
    private final Clock clock;
    private final QuoteViews views;

    public ListQuotesUseCase(UnitOfWork unitOfWork, ContactRepository contacts, VehicleRepository vehicles,
                             QuoteRepository quotes, OwnerGuard guard, Clock clock) {
        this.unitOfWork = unitOfWork;
        this.quotes = quotes;
        this.guard = guard;
        this.clock = clock;
        this.views = new QuoteViews(contacts, vehicles, clock);
    }

    public QuoteQueue execute(ListQuotesQuery query) {
        CompanyId companyId = new CompanyId(query.principal().companyId());
        UserId userId = new UserId(query.principal().userId());
        return unitOfWork.execute(new AccessScope.Tenant(userId, companyId), () -> {
            guard.require(query.principal());
            Instant now = clock.instant();
            List<Quote> sent = quotes.findByStatuses(companyId, SENT, OPEN_LIMIT);
            List<Quote> closed = new ArrayList<>(quotes.findByStatuses(companyId, CLOSED, CLOSED_LIMIT));
            sent.stream().filter(quote -> quote.statusAt(now) == QuoteStatus.EXPIRED).forEach(closed::add);
            closed.sort(Comparator.comparing(Quote::createdAt).reversed());

            return new QuoteQueue(
                    quotes.findByStatuses(companyId, NEEDS_OWNER, OPEN_LIMIT).stream().map(views::summary).toList(),
                    sent.stream().filter(quote -> quote.statusAt(now) == QuoteStatus.SENT).map(views::summary).toList(),
                    closed.stream().limit(CLOSED_LIMIT).map(views::summary).toList());
        });
    }
}
