package br.com.erpcore.domain.contact;

import java.util.Objects;
import java.util.UUID;

public record ContactId(UUID value) {

    public ContactId {
        Objects.requireNonNull(value, "value");
    }
}
