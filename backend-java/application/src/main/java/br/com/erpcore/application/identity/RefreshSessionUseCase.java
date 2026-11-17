package br.com.erpcore.application.identity;

import br.com.erpcore.application.audit.AuditRecorder;
import br.com.erpcore.application.identity.port.out.MembershipRepository;
import br.com.erpcore.application.identity.port.out.RefreshTokenCodec;
import br.com.erpcore.application.identity.port.out.RefreshTokenRepository;
import br.com.erpcore.application.shared.port.out.AccessScope;
import br.com.erpcore.application.shared.port.out.UnitOfWork;
import br.com.erpcore.domain.audit.AuditEventType;
import br.com.erpcore.domain.identity.InvalidRefreshTokenException;
import br.com.erpcore.domain.identity.Membership;
import br.com.erpcore.domain.identity.RefreshToken;
import br.com.erpcore.domain.identity.TokenHash;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Renova a sessão trocando o refresh token por um novo (rotação). Usar de novo um token já trocado indica
 * roubo ou cópia: a sessão inteira é revogada.
 */
public final class RefreshSessionUseCase {

    private static final String REASON_UNKNOWN_TOKEN = "UNKNOWN_TOKEN";
    private static final String REASON_REUSED = "REVOKED_TOKEN_REUSED";
    private static final String REASON_EXPIRED = "EXPIRED";
    private static final String REASON_NO_MEMBERSHIP = "NO_MEMBERSHIP";

    private final UnitOfWork unitOfWork;
    private final RefreshTokenRepository refreshTokens;
    private final MembershipRepository memberships;
    private final RefreshTokenCodec refreshCodec;
    private final SessionIssuer sessions;
    private final AuditRecorder audit;
    private final Clock clock;

    public RefreshSessionUseCase(UnitOfWork unitOfWork, RefreshTokenRepository refreshTokens,
                                 MembershipRepository memberships, RefreshTokenCodec refreshCodec,
                                 SessionIssuer sessions, AuditRecorder audit, Clock clock) {
        this.unitOfWork = unitOfWork;
        this.refreshTokens = refreshTokens;
        this.memberships = memberships;
        this.refreshCodec = refreshCodec;
        this.sessions = sessions;
        this.audit = audit;
        this.clock = clock;
    }

    public SessionTokens execute(RefreshSessionCommand command) {
        TokenHash hash = new TokenHash(refreshCodec.hash(Objects.toString(command.refreshToken(), "")));

        SessionOutcome outcome = unitOfWork.execute(new AccessScope.RefreshTokenLookup(hash),
                () -> rotate(hash, command.correlationId()));

        return switch (outcome) {
            case SessionOutcome.Succeeded succeeded -> succeeded.tokens();
            case SessionOutcome.Rejected rejected -> throw new InvalidRefreshTokenException();
        };
    }

    private SessionOutcome rotate(TokenHash hash, String correlationId) {
        Optional<RefreshToken> found = refreshTokens.findByHash(hash);
        if (found.isEmpty()) {
            audit.failure(AuditEventType.TOKEN_REFRESH, null, null, correlationId, REASON_UNKNOWN_TOKEN);
            return new SessionOutcome.Rejected();
        }

        RefreshToken token = found.get();
        Instant now = clock.instant();
        if (token.isRevoked()) {
            refreshTokens.revokeFamily(token.familyId(), now);
            audit.failure(AuditEventType.TOKEN_REUSE_DETECTED, token.companyId(), token.userId(), correlationId,
                    REASON_REUSED);
            return new SessionOutcome.Rejected();
        }
        if (token.isExpiredAt(now)) {
            audit.failure(AuditEventType.TOKEN_REFRESH, token.companyId(), token.userId(), correlationId,
                    REASON_EXPIRED);
            return new SessionOutcome.Rejected();
        }

        Optional<Membership> membership = memberships.find(token.companyId(), token.userId());
        if (membership.isEmpty()) {
            refreshTokens.revokeFamily(token.familyId(), now);
            audit.failure(AuditEventType.TOKEN_REFRESH, token.companyId(), token.userId(), correlationId,
                    REASON_NO_MEMBERSHIP);
            return new SessionOutcome.Rejected();
        }

        SessionTokens tokens = sessions.continueSession(token, membership.get(), now);
        audit.success(AuditEventType.TOKEN_REFRESH, token.companyId(), token.userId(), correlationId);
        return new SessionOutcome.Succeeded(tokens);
    }
}
