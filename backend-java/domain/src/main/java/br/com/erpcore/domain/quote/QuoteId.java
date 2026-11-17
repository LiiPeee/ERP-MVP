package br.com.erpcore.domain.quote;

import java.util.Objects;
import java.util.UUID;

public record QuoteId(UUID value) {

    public QuoteId {
        Objects.requireNonNull(value, "value");
    }
}
