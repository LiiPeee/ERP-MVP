package br.com.erpcore.domain.quote;

import br.com.erpcore.domain.shared.DomainValidationException;
import br.com.erpcore.domain.shared.Money;
import br.com.erpcore.domain.shared.Quantity;
import br.com.erpcore.domain.shared.Validation;
import java.util.Objects;

/** Uma linha do orçamento. O total da linha é calculado aqui, nunca vem de fora (princípio: todo número do domínio). */
public record QuoteItem(String description, QuoteItemKind kind, Quantity quantity, Money unitPrice) {

    public static final int DESCRIPTION_MAX_LENGTH = 200;
    public static final Money MAX_UNIT_PRICE = Money.of("1000000.00");

    public QuoteItem {
        description = Validation.requireTrimmedText("description", description, DESCRIPTION_MAX_LENGTH);
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(quantity, "quantity");
        Objects.requireNonNull(unitPrice, "unitPrice");
        if (unitPrice.amount().signum() < 0 || unitPrice.compareTo(MAX_UNIT_PRICE) > 0) {
            throw new DomainValidationException("unitPrice", Validation.OUT_OF_RANGE);
        }
    }

    public static QuoteItem of(String description, QuoteItemKind kind, Quantity quantity, Money unitPrice) {
        return new QuoteItem(description, kind, quantity, unitPrice);
    }

    public Money lineTotal() {
        return unitPrice.multiply(quantity);
    }
}
