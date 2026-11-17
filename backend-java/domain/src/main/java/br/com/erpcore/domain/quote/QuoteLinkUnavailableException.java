package br.com.erpcore.domain.quote;

import br.com.erpcore.domain.shared.DomainException;

/**
 * O link não serve mais ou nunca serviu. É uma só exceção para link inexistente, vencido ou de orçamento
 * cancelado: de fora, ninguém distingue um do outro.
 */
public final class QuoteLinkUnavailableException extends DomainException {

    public QuoteLinkUnavailableException() {
        super("Quote link unavailable");
    }
}
