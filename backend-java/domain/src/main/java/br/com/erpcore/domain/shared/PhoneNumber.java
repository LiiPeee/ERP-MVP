package br.com.erpcore.domain.shared;

import java.util.regex.Pattern;

/**
 * Telefone brasileiro, guardado em E.164 ({@code +55DDNNNNNNNNN}). É dado pessoal de terceiro: cifrado no banco,
 * mascarado na API e fora dos logs ({@code toString} é mascarado).
 * <p>
 * Regra de forma, não de operadora: DDD de dois dígitos sem zero, celular de 9 dígitos começando em 9 ou fixo de 8
 * dígitos começando de 2 a 5. Número de outro país é recusado nesta fase.
 */
public record PhoneNumber(String value) {

    private static final String FIELD = "phone";
    private static final String COUNTRY_CODE = "55";
    private static final Pattern ALLOWED_CHARACTERS = Pattern.compile("^\\+?[0-9 ()\\-.]+$");
    private static final Pattern E164 = Pattern.compile("^\\+55[1-9][1-9](9\\d{8}|[2-5]\\d{7})$");
    private static final int NATIONAL_LENGTH_LANDLINE = 10;
    private static final int NATIONAL_LENGTH_MOBILE = 11;
    private static final int AREA_CODE_LENGTH = 2;
    private static final int VISIBLE_LAST_DIGITS = 4;

    public PhoneNumber {
        if (value == null || !E164.matcher(value).matches()) {
            throw new DomainValidationException(FIELD, Validation.INVALID_FORMAT);
        }
    }

    public static PhoneNumber of(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new DomainValidationException(FIELD, Validation.REQUIRED);
        }
        String text = raw.trim();
        if (!ALLOWED_CHARACTERS.matcher(text).matches()) {
            throw new DomainValidationException(FIELD, Validation.INVALID_FORMAT);
        }
        String digits = text.replaceAll("\\D", "");
        boolean hasCountryCode = text.startsWith("+")
                || ((digits.length() == NATIONAL_LENGTH_LANDLINE + 2 || digits.length() == NATIONAL_LENGTH_MOBILE + 2)
                && digits.startsWith(COUNTRY_CODE));
        if (hasCountryCode) {
            if (!digits.startsWith(COUNTRY_CODE)) {
                throw new DomainValidationException(FIELD, Validation.INVALID_FORMAT);
            }
            digits = digits.substring(COUNTRY_CODE.length());
        }
        return new PhoneNumber("+" + COUNTRY_CODE + digits);
    }

    /** Só o DDD e os quatro últimos dígitos, por exemplo {@code (11) *****-4321}. */
    public String masked() {
        String national = value.substring(COUNTRY_CODE.length() + 1);
        String areaCode = national.substring(0, AREA_CODE_LENGTH);
        String local = national.substring(AREA_CODE_LENGTH);
        String hidden = "*".repeat(local.length() - VISIBLE_LAST_DIGITS);
        return "(" + areaCode + ") " + hidden + "-" + local.substring(local.length() - VISIBLE_LAST_DIGITS);
    }

    @Override
    public String toString() {
        return "PhoneNumber[redacted]";
    }
}
