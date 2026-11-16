package br.com.erpcore.domain.identity;

import br.com.erpcore.domain.shared.Validation;

public record DisplayName(String value) {

    public static final int MAX_LENGTH = 120;

    public DisplayName {
        value = Validation.requireTrimmedText("displayName", value, MAX_LENGTH);
    }

    public static DisplayName of(String raw) {
        return new DisplayName(raw);
    }
}
