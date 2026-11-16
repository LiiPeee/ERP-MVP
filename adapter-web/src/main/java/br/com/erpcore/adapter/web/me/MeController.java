package br.com.erpcore.adapter.web.me;

import br.com.erpcore.adapter.web.correlation.CorrelationIds;
import br.com.erpcore.adapter.web.security.TokenClaims;
import br.com.erpcore.application.identity.AuthenticatedPrincipal;
import br.com.erpcore.application.identity.CurrentContext;
import br.com.erpcore.application.identity.GetCurrentContextQuery;
import br.com.erpcore.application.identity.GetCurrentContextUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Me")
final class MeController {

    private final GetCurrentContextUseCase currentContext;

    MeController(GetCurrentContextUseCase currentContext) {
        this.currentContext = currentContext;
    }

    /** O usuário e a empresa vêm só do token já validado; nenhum parâmetro da requisição escolhe o tenant. */
    @GetMapping(path = "/api/me", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Quem sou eu: usuário, empresa e papel")
    @SecurityRequirement(name = "bearerAuth")
    MeResponse me(@AuthenticationPrincipal Jwt token) {
        AuthenticatedPrincipal principal = new AuthenticatedPrincipal(UUID.fromString(token.getSubject()),
                UUID.fromString(token.getClaimAsString(TokenClaims.COMPANY)));
        CurrentContext context = currentContext.execute(new GetCurrentContextQuery(principal, CorrelationIds.current()));
        return new MeResponse(context.userId(), context.email(), context.displayName(), context.companyId(),
                context.companyName(), context.role());
    }

    record MeResponse(UUID userId, String email, String displayName, UUID companyId, String companyName, String role) {

        @Override
        public String toString() {
            return "MeResponse[redacted]";
        }
    }
}
