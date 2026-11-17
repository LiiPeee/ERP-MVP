package br.com.erpcore.application.quote;

import java.util.List;

/** A fila: o que precisa do dono, o que espera o cliente e o que já fechou. */
public record QuoteQueue(List<QuoteSummary> needsOwner, List<QuoteSummary> awaitingClient, List<QuoteSummary> closed) {
}
