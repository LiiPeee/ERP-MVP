package br.com.erpcore.application.quote;

/** O estado do orçamento depois da decisão: {@code APPROVED} ou {@code REJECTED}. */
public record PublicQuoteDecision(String status) {
}
