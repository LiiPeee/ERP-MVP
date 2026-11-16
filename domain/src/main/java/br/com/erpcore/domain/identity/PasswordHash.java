package br.com.erpcore.domain.identity;

import br.com.erpcore.domain.shared.DomainValidationException;
import br.com.erpcore.domain.shared.Validation;

public record PasswordHash(String value) {

    public PasswordHash {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException("passwordHash", Validation.REQUIRED);
        }
    }

    @Override
    public String toString() {
        return "PasswordHash[redacted]";
    }
}
