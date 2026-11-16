package br.com.erpcore.domain.identity;

import java.util.Objects;
import java.util.UUID;

public record MembershipId(UUID value) {

    public MembershipId {
        Objects.requireNonNull(value, "value");
    }
}
