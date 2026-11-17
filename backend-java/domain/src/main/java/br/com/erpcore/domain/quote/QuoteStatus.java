package br.com.erpcore.domain.quote;

/**
 * Estados do orçamento: {@code DRAFT -> AWAITING_OWNER_CONFIRMATION -> SENT -> APPROVED | REJECTED | EXPIRED}, e
 * {@code CANCELLED} a partir de qualquer estado que ainda não foi decidido.
 */
public enum QuoteStatus {
    DRAFT,
    AWAITING_OWNER_CONFIRMATION,
    SENT,
    APPROVED,
    REJECTED,
    EXPIRED,
    CANCELLED;

    public boolean isFinal() {
        return this == APPROVED || this == REJECTED || this == EXPIRED || this == CANCELLED;
    }
}
