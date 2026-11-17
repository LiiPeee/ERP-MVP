package br.com.erpcore.domain.quote;

import br.com.erpcore.domain.shared.DomainException;

/** A ação não é permitida no estado atual do orçamento. */
public final class InvalidQuoteTransitionException extends DomainException {

    private final QuoteStatus from;
    private final String action;

    public InvalidQuoteTransitionException(QuoteStatus from, String action) {
        super("QUOTE_INVALID_TRANSITION: " + action + " from " + from);
        this.from = from;
        this.action = action;
    }

    public QuoteStatus from() {
        return from;
    }

    public String action() {
        return action;
    }
}
