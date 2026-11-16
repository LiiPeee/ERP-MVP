package br.com.erpcore.application.identity;

import java.util.UUID;

/** Quem está autenticado, como veio do token de acesso já validado. É a única fonte do tenant. */
public record AuthenticatedPrincipal(UUID userId, UUID companyId) {
}
