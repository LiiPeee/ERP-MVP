package br.com.erpcore.adapter.web.quote;

import br.com.erpcore.application.quote.QuoteItemInput;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/** Corpos das rotas de orçamento. Só o formato e o tamanho são conferidos aqui; as regras são do domínio. */
final class QuoteDtos {

    private static final int MAX_ITEMS = 100;
    private static final int MAX_DESCRIPTION = 300;
    private static final int MAX_NUMBER_TEXT = 30;
    private static final int MAX_NOTE = 1000;

    private QuoteDtos() {
    }

    /** Quantidade e preço são texto decimal exato ("1.5", "89.90"): nunca número de ponto flutuante. */
    record QuoteItemRequest(@Size(max = MAX_DESCRIPTION) String description, @Size(max = MAX_NUMBER_TEXT) String kind,
                       @Size(max = MAX_NUMBER_TEXT) String quantity, @Size(max = MAX_NUMBER_TEXT) String unitPrice) {

        QuoteItemInput toInput() {
            return new QuoteItemInput(description, kind, quantity, unitPrice);
        }
    }

    record CreateQuoteRequest(UUID contactId, UUID vehicleId, @Size(max = MAX_ITEMS) List<@Valid QuoteItemRequest> items,
                         @Size(max = MAX_NOTE) String note, Integer validityDays) {

        List<QuoteItemInput> inputs() {
            return QuoteDtos.inputs(items);
        }
    }

    record ReviseQuoteItemsRequest(@Size(max = MAX_ITEMS) List<@Valid QuoteItemRequest> items) {

        List<QuoteItemInput> inputs() {
            return QuoteDtos.inputs(items);
        }
    }

    record RejectQuoteRequest(@Size(max = MAX_NOTE) String reason) {

        @Override
        public String toString() {
            return "RejectQuoteRequest[redacted]";
        }
    }

    private static List<QuoteItemInput> inputs(List<QuoteItemRequest> items) {
        return items == null ? List.of() : items.stream().map(QuoteItemRequest::toInput).toList();
    }
}
