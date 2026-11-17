package br.com.erpcore.application.quote;

import br.com.erpcore.application.identity.AuthenticatedPrincipal;
import java.util.UUID;

public record ConfirmQuoteCommand(AuthenticatedPrincipal principal, UUID quoteId, String correlationId) {
}
