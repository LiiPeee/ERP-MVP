package br.com.erpcore.domain.company;

import br.com.erpcore.domain.shared.PhoneNumber;
import br.com.erpcore.domain.shared.DomainValidationException;
import br.com.erpcore.domain.shared.Validation;
import java.util.Optional;

/**
 * Como a oficina se apresenta ao cliente e a validade padrão dos orçamentos. Textos opcionais ficam vazios quando
 * não informados.
 */
public record CompanyProfile(CompanyId companyId, String displayName, Optional<PhoneNumber> businessPhone,
                             String address, String openingHours, int defaultValidityDays, String signatureMessage) {

    public static final int DEFAULT_VALIDITY_DAYS = 7;
    public static final int MAX_VALIDITY_DAYS = 90;
    public static final int DISPLAY_NAME_MAX_LENGTH = 120;
    public static final int ADDRESS_MAX_LENGTH = 200;
    public static final int HOURS_MAX_LENGTH = 200;
    public static final int SIGNATURE_MAX_LENGTH = 300;

    public static CompanyProfile of(CompanyId companyId, String displayName, PhoneNumber businessPhone, String address,
                                    String openingHours, int defaultValidityDays, String signatureMessage) {
        if (defaultValidityDays < 1 || defaultValidityDays > MAX_VALIDITY_DAYS) {
            throw new DomainValidationException("defaultValidityDays", Validation.OUT_OF_RANGE);
        }
        return new CompanyProfile(companyId,
                Validation.requireTrimmedText("displayName", displayName, DISPLAY_NAME_MAX_LENGTH),
                Optional.ofNullable(businessPhone),
                optionalText("address", address, ADDRESS_MAX_LENGTH),
                optionalText("openingHours", openingHours, HOURS_MAX_LENGTH),
                defaultValidityDays,
                optionalText("signatureMessage", signatureMessage, SIGNATURE_MAX_LENGTH));
    }

    /** O perfil de uma oficina que ainda não preencheu nada: o nome da empresa e a validade padrão. */
    public static CompanyProfile defaultFor(Company company) {
        return of(company.id(), company.name().value(), null, null, null, DEFAULT_VALIDITY_DAYS, null);
    }

    private static String optionalText(String field, String raw, int maxLength) {
        return raw == null || raw.isBlank() ? "" : Validation.requireTrimmedText(field, raw, maxLength);
    }
}
