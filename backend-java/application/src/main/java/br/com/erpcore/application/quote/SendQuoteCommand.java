package br.com.erpcore.application.quote;

import br.com.erpcore.application.identity.AuthenticatedPrincipal;
import java.util.UUID;

public record SendQuoteCommand(AuthenticatedPrincipal principal, UUID quoteId, String correlationId) {
}
