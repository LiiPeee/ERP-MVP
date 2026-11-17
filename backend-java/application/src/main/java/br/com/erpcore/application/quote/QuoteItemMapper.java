package br.com.erpcore.application.quote;

import br.com.erpcore.domain.quote.QuoteItem;
import br.com.erpcore.domain.quote.QuoteItemKind;
import br.com.erpcore.domain.shared.DomainValidationException;
import br.com.erpcore.domain.shared.Money;
import br.com.erpcore.domain.shared.Quantity;
import br.com.erpcore.domain.shared.Validation;
import java.util.ArrayList;
import java.util.List;

/** Converte os itens de texto em itens de domínio. O erro diz qual item e qual campo: {@code items[1].quantity}. */
final class QuoteItemMapper {

    private QuoteItemMapper() {
    }

    static List<QuoteItem> toDomain(List<QuoteItemInput> inputs) {
        List<QuoteItem> items = new ArrayList<>();
        List<QuoteItemInput> source = inputs == null ? List.of() : inputs;
        for (int index = 0; index < source.size(); index++) {
            QuoteItemInput input = source.get(index);
            try {
                items.add(QuoteItem.of(input.description(), parseKind(input.kind()), Quantity.of(input.quantity()),
                        parseUnitPrice(input.unitPrice())));
            } catch (DomainValidationException failure) {
                throw new DomainValidationException("items[" + index + "]." + failure.field(), failure.code());
            }
        }
        return items;
    }

    private static QuoteItemKind parseKind(String raw) {
        try {
            return QuoteItemKind.valueOf(raw == null ? "" : raw.trim());
        } catch (IllegalArgumentException unknownKind) {
            throw new DomainValidationException("kind", Validation.INVALID_FORMAT);
        }
    }

    /** O texto do preço é "amount" no domínio; na API o campo se chama {@code unitPrice}. */
    private static Money parseUnitPrice(String raw) {
        try {
            return Money.of(raw);
        } catch (DomainValidationException failure) {
            throw new DomainValidationException("unitPrice", failure.code());
        }
    }
}
