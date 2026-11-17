package br.com.erpcore.adapter.web.security;

import br.com.erpcore.application.identity.AuthenticatedPrincipal;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

/** O usuário e a empresa que os casos de uso recebem vêm só do token já validado, nunca da requisição. */
public final class Principals {

    private Principals() {
    }

    public static AuthenticatedPrincipal of(Jwt token) {
        return new AuthenticatedPrincipal(UUID.fromString(token.getSubject()),
                UUID.fromString(token.getClaimAsString(TokenClaims.COMPANY)));
    }
}
