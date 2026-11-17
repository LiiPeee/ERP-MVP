package br.com.erpcore.domain.quote;

import br.com.erpcore.domain.shared.DomainException;

public final class QuoteNotFoundException extends DomainException {

    public QuoteNotFoundException() {
        super("Quote not found");
    }
}
