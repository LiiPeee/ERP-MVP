package br.com.erpcore.domain.contact;

import br.com.erpcore.domain.shared.Validation;

/** Nome do cliente da oficina. É dado pessoal: nunca aparece em texto de log ({@code toString} é mascarado). */
public record ContactName(String value) {

    public static final int MAX_LENGTH = 120;

    public ContactName {
        value = Validation.requireTrimmedText("contactName", value, MAX_LENGTH);
    }

    public static ContactName of(String raw) {
        return new ContactName(raw);
    }

    @Override
    public String toString() {
        return "ContactName[redacted]";
    }
}
