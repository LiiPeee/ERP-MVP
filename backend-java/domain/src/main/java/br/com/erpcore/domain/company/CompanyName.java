package br.com.erpcore.domain.company;

import br.com.erpcore.domain.shared.Validation;

public record CompanyName(String value) {

    public static final int MAX_LENGTH = 120;

    public CompanyName {
        value = Validation.requireTrimmedText("companyName", value, MAX_LENGTH);
    }

    public static CompanyName of(String raw) {
        return new CompanyName(raw);
    }
}
