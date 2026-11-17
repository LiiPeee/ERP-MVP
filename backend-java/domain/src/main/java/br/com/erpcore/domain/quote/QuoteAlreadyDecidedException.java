package br.com.erpcore.domain.quote;

import br.com.erpcore.domain.shared.DomainException;

/** Quem tem o link já respondeu o contrário: aprovou e agora quer recusar, ou o inverso. */
public final class QuoteAlreadyDecidedException extends DomainException {

    public QuoteAlreadyDecidedException() {
        super("Quote already decided");
    }
}
