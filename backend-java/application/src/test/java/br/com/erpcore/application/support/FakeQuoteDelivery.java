package br.com.erpcore.application.support;

import br.com.erpcore.application.quote.port.out.QuoteDelivery;
import br.com.erpcore.application.quote.port.out.QuoteDeliveryRequest;
import java.util.ArrayList;
import java.util.List;

/** Guarda o que seria entregue e pode ser mandado falhar, para provar que a falha não desfaz o envio. */
public final class FakeQuoteDelivery implements QuoteDelivery {

    private final List<QuoteDeliveryRequest> delivered = new ArrayList<>();
    private boolean failing;

    @Override
    public void deliver(QuoteDeliveryRequest request) {
        if (failing) {
            throw new IllegalStateException("delivery channel down");
        }
        delivered.add(request);
    }

    public void failFromNowOn() {
        failing = true;
    }

    public List<QuoteDeliveryRequest> delivered() {
        return List.copyOf(delivered);
    }

    public QuoteDeliveryRequest last() {
        return delivered.get(delivered.size() - 1);
    }
}
