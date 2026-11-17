package br.com.erpcore.adapter.infra.quote;

import br.com.erpcore.application.quote.port.out.QuoteDelivery;
import br.com.erpcore.application.quote.port.out.QuoteDeliveryRequest;

/**
 * Enquanto não há canal automático (o WhatsApp oficial vem na Fase 2), nada é entregue sozinho: falhar aqui faz o
 * resultado dizer {@code delivered = false}, e o dono compartilha o link que a tela mostra.
 */
public final class ManualQuoteDelivery implements QuoteDelivery {

    @Override
    public void deliver(QuoteDeliveryRequest request) {
        throw new IllegalStateException("no automatic delivery channel is configured yet");
    }
}
