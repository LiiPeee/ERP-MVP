package br.com.erpcore.domain.identity;

import br.com.erpcore.domain.shared.DomainValidationException;
import br.com.erpcore.domain.shared.Validation;

/** Resumo (hash) de um refresh token. O token em si nunca é guardado. */
public record TokenHash(String value) {

    public TokenHash {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException("tokenHash", Validation.REQUIRED);
        }
    }

    @Override
    public String toString() {
        return "TokenHash[redacted]";
    }
}
