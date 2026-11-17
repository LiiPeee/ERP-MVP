package br.com.erpcore.application.quote;

/** Um item como chega de fora: tudo texto, validado no domínio. O total da linha nunca vem de fora. */
public record QuoteItemInput(String description, String kind, String quantity, String unitPrice) {
}
