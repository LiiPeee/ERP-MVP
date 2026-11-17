package br.com.erpcore.bootstrap;

import br.com.erpcore.application.quote.ExpireDueQuotesUseCase;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * A tarefa que grava como vencidos os orçamentos enviados fora do prazo. O cliente já não consegue decidir depois do
 * prazo (o domínio confere a cada decisão); isto só mantém o estado gravado e a fila certos. É uma aplicação só,
 * então há uma tarefa só: nada de fila, agendador externo ou trava distribuída.
 */
@Component
final class ExpireDueQuotesJob {

    private static final Logger LOG = LoggerFactory.getLogger(ExpireDueQuotesJob.class);

    private final ExpireDueQuotesUseCase expireDueQuotes;

    ExpireDueQuotesJob(ExpireDueQuotesUseCase expireDueQuotes) {
        this.expireDueQuotes = expireDueQuotes;
    }

    /** Devolve quantos orçamentos venceram nesta execução; uma falha é registrada sem detalhe e tentada de novo no próximo ciclo. */
    @Scheduled(initialDelayString = "${erp.business.expiry-initial-delay:PT1M}",
            fixedDelayString = "${erp.business.expiry-interval:PT5M}")
    int run() {
        try {
            int expired = expireDueQuotes.execute(UUID.randomUUID().toString());
            if (expired > 0) {
                LOG.info("quotes_expired count={}", expired);
            }
            return expired;
        } catch (RuntimeException failure) {
            LOG.error("quote expiry job failed: {}", failure.getClass().getSimpleName());
            return 0;
        }
    }
}
