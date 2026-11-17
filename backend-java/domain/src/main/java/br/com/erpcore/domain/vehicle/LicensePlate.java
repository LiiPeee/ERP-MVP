package br.com.erpcore.domain.vehicle;

import br.com.erpcore.domain.shared.DomainValidationException;
import br.com.erpcore.domain.shared.Validation;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Placa no formato antigo ({@code ABC1234}) ou Mercosul ({@code ABC1D23}), em maiúsculas e sem hífen. A placa
 * identifica uma pessoa: é cifrada no banco, nunca aparece completa para o cliente e fica fora dos logs.
 */
public record LicensePlate(String value) {

    private static final String FIELD = "licensePlate";
    private static final Pattern FORMAT = Pattern.compile("^[A-Z]{3}[0-9]([0-9]|[A-Z])[0-9]{2}$");
    private static final int VISIBLE_CHARACTERS = 3;

    public LicensePlate {
        if (value == null || !FORMAT.matcher(value).matches()) {
            throw new DomainValidationException(FIELD, Validation.INVALID_FORMAT);
        }
    }

    public static LicensePlate of(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new DomainValidationException(FIELD, Validation.REQUIRED);
        }
        return new LicensePlate(raw.trim().replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT));
    }

    /** O que a página pública mostra: os três últimos caracteres. */
    public String lastCharacters() {
        return value.substring(value.length() - VISIBLE_CHARACTERS);
    }

    @Override
    public String toString() {
        return "LicensePlate[redacted]";
    }
}
