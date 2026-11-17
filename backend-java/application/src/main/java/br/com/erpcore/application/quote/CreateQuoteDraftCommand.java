package br.com.erpcore.application.quote;

import br.com.erpcore.application.identity.AuthenticatedPrincipal;
import java.util.List;
import java.util.UUID;

/** {@code validityDays} nulo usa o padrão da oficina. {@code note} pode ser nula. */
public record CreateQuoteDraftCommand(AuthenticatedPrincipal principal, UUID contactId, UUID vehicleId,
                                      List<QuoteItemInput> items, String note, Integer validityDays,
                                      String correlationId) {
}
