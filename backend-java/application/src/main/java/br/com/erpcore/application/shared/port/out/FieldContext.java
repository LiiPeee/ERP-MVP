package br.com.erpcore.application.shared.port.out;

import br.com.erpcore.domain.company.CompanyId;
import java.util.Objects;

/** A que empresa e a que campo o valor pertence (por exemplo {@code contact.phone}). */
public record FieldContext(CompanyId companyId, String field) {

    public FieldContext {
        Objects.requireNonNull(companyId, "companyId");
        if (field == null || field.isBlank()) {
            throw new IllegalArgumentException("field is required");
        }
    }

    /** Os dados associados que a cifra autentica. */
    public String associatedData() {
        return companyId.value() + "|" + field;
    }
}
