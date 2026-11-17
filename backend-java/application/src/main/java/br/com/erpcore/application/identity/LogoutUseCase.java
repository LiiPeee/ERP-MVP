package br.com.erpcore.application.identity;

import br.com.erpcore.application.audit.AuditRecorder;
import br.com.erpcore.application.identity.port.out.RefreshTokenCodec;
import br.com.erpcore.application.identity.port.out.RefreshTokenRepository;
import br.com.erpcore.application.shared.port.out.AccessScope;
import br.com.erpcore.application.shared.port.out.UnitOfWork;
import br.com.erpcore.domain.audit.AuditEventType;
import br.com.erpcore.domain.identity.RefreshToken;
import br.com.erpcore.domain.identity.TokenHash;
import java.time.Clock;
import java.util.Objects;
import java.util.Optional;

/**
 * Encerra a sessão revogando o refresh token e todos os da mesma família. É idempotente: sair duas vezes, ou
 * com um token desconhecido, não dá erro (o cliente só quer a sessão encerrada), mas a tentativa é auditada.
 */
public final class LogoutUseCase {

    private static final String REASON_UNKNOWN_TOKEN = "UNKNOWN_TOKEN";

    private final UnitOfWork unitOfWork;
    private final RefreshTokenRepository refreshTokens;
    private final RefreshTokenCodec refreshCodec;
    private final AuditRecorder audit;
    private final Clock clock;

    public LogoutUseCase(UnitOfWork unitOfWork, RefreshTokenRepository refreshTokens, RefreshTokenCodec refreshCodec,
                         AuditRecorder audit, Clock clock) {
        this.unitOfWork = unitOfWork;
        this.refreshTokens = refreshTokens;
        this.refreshCodec = refreshCodec;
        this.audit = audit;
        this.clock = clock;
    }

    public void execute(LogoutCommand command) {
        TokenHash hash = new TokenHash(refreshCodec.hash(Objects.toString(command.refreshToken(), "")));

        unitOfWork.execute(new AccessScope.RefreshTokenLookup(hash), () -> {
            endSession(hash, command.correlationId());
            return null;
        });
    }

    private void endSession(TokenHash hash, String correlationId) {
        Optional<RefreshToken> found = refreshTokens.findByHash(hash);
        if (found.isEmpty()) {
            audit.failure(AuditEventType.LOGOUT, null, null, correlationId, REASON_UNKNOWN_TOKEN);
            return;
        }
        RefreshToken token = found.get();
        refreshTokens.revokeFamily(token.familyId(), clock.instant());
        audit.success(AuditEventType.LOGOUT, token.companyId(), token.userId(), correlationId);
    }
}
