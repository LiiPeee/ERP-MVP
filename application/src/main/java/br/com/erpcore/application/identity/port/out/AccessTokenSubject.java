package br.com.erpcore.application.identity.port.out;

import java.util.UUID;

/** Quem o token de acesso representa. O tenant (empresa) sai sempre daqui, nunca de um parâmetro do cliente. */
public record AccessTokenSubject(UUID userId, UUID companyId, String role) {
}
