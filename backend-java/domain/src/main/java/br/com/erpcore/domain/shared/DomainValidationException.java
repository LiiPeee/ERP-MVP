package br.com.erpcore.domain.shared;

/** Um campo de entrada violou uma regra. O código é estável e o texto de tela é decidido por quem apresenta o erro. */
public final class DomainValidationException extends DomainException {

    private final String field;
    private final String code;

    public DomainValidationException(String field, String code) {
        super(field + ": " + code);
        this.field = field;
        this.code = code;
    }

    public String field() {
        return field;
    }

    public String code() {
        return code;
    }
}
