package br.com.erpcore.domain.identity;

import java.util.Objects;
import java.util.UUID;

public record TokenFamilyId(UUID value) {

    public TokenFamilyId {
        Objects.requireNonNull(value, "value");
    }
}
