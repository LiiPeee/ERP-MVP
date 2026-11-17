package br.com.erpcore.application.quote.port.out;

import br.com.erpcore.domain.company.CompanyId;
import br.com.erpcore.domain.quote.Quote;
import br.com.erpcore.domain.quote.QuoteId;
import br.com.erpcore.domain.quote.QuoteStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface QuoteRepository {

    /** Insere o orçamento (com os itens) ou atualiza o que já existe com o mesmo id. */
    void save(Quote quote);

    Optional<Quote> findById(CompanyId companyId, QuoteId id);

    /**
     * Como {@link #findById}, mas trava a linha até o fim da transação: duas decisões ao mesmo tempo (aprovar e
     * recusar) se enfileiram em vez de se atropelar.
     */
    Optional<Quote> findByIdForUpdate(CompanyId companyId, QuoteId id);

    /** Do mais novo ao mais antigo, no máximo {@code limit}. */
    List<Quote> findByStatuses(CompanyId companyId, Set<QuoteStatus> statuses, int limit);

    /** De todas as oficinas: orçamentos enviados cujo prazo já acabou. Só a tarefa de expiração usa. */
    List<Quote> findDueForExpiry(Instant now, int limit);
}
