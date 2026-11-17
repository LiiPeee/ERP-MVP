package br.com.erpcore.domain.quote;

import br.com.erpcore.domain.shared.DomainException;

/** O prazo do orçamento já passou: o cliente não pode mais decidir. */
public final class QuoteUnavailableException extends DomainException {

    public QuoteUnavailableException() {
        super("QUOTE_UNAVAILABLE");
    }
}
