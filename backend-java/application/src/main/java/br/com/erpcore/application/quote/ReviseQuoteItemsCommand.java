package br.com.erpcore.application.quote;

import br.com.erpcore.application.identity.AuthenticatedPrincipal;
import java.util.List;
import java.util.UUID;

/** Troca todos os itens do orçamento (a lista nova substitui a antiga). */
public record ReviseQuoteItemsCommand(AuthenticatedPrincipal principal, UUID quoteId, List<QuoteItemInput> items,
                                      String correlationId) {
}
