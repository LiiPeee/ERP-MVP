package br.com.erpcore.application.quote;

import br.com.erpcore.domain.quote.QuoteItem;

/** Valores em reais como texto exato ("89.90"), calculados pelo domínio. */
public record QuoteItemView(String description, String kind, String quantity, String unitPrice, String lineTotal) {

    static QuoteItemView from(QuoteItem item) {
        return new QuoteItemView(item.description(), item.kind().name(), item.quantity().toString(),
                item.unitPrice().toString(), item.lineTotal().toString());
    }
}
