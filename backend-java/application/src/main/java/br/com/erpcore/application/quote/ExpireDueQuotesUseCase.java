package br.com.erpcore.application.quote;

import br.com.erpcore.application.audit.AuditRecorder;
import br.com.erpcore.application.quote.port.out.QuoteRepository;
import br.com.erpcore.application.shared.port.out.AccessScope;
import br.com.erpcore.application.shared.port.out.UnitOfWork;
import br.com.erpcore.domain.audit.AuditEventType;
import br.com.erpcore.domain.quote.Quote;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Tarefa agendada: marca como vencidos os orçamentos enviados cujo prazo acabou, em todas as empresas, em lotes. O
 * cliente já não consegue decidir depois do prazo (o domínio confere); esta tarefa só deixa o estado gravado certo.
 */
public final class ExpireDueQuotesUseCase {

    private final UnitOfWork unitOfWork;
    private final QuoteRepository quotes;
    private final AuditRecorder audit;
    private final Clock clock;
    private final int batchSize;

    public ExpireDueQuotesUseCase(UnitOfWork unitOfWork, QuoteRepository quotes, AuditRecorder audit, Clock clock,
                                  int batchSize) {
        this.unitOfWork = unitOfWork;
        this.quotes = quotes;
        this.audit = audit;
        this.clock = clock;
        this.batchSize = batchSize;
    }

    /** Devolve quantos orçamentos foram vencidos nesta execução. */
    public int execute(String correlationId) {
        Instant now = clock.instant();
        int expired = 0;
        int inBatch;
        do {
            inBatch = unitOfWork.execute(new AccessScope.QuoteExpiration(), () -> expireBatch(now, correlationId));
            expired += inBatch;
        } while (inBatch == batchSize);
        return expired;
    }

    private int expireBatch(Instant now, String correlationId) {
        List<Quote> due = quotes.findDueForExpiry(now, batchSize);
        due.forEach(quote -> {
            if (quote.expireIfDue(now)) {
                quotes.save(quote);
                audit.systemSuccess(AuditEventType.QUOTE_EXPIRED, quote.companyId(), correlationId,
                        Map.of("quoteId", quote.id().value().toString()));
            }
        });
        return due.size();
    }
}
