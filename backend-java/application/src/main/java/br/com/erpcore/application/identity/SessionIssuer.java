package br.com.erpcore.application.identity;

import br.com.erpcore.application.identity.port.out.AccessTokenIssuer;
import br.com.erpcore.application.identity.port.out.AccessTokenSubject;
import br.com.erpcore.application.identity.port.out.IssuedAccessToken;
import br.com.erpcore.application.identity.port.out.NewRefreshSecret;
import br.com.erpcore.application.identity.port.out.RefreshTokenCodec;
import br.com.erpcore.application.identity.port.out.RefreshTokenRepository;
import br.com.erpcore.application.shared.port.out.IdGenerator;
import br.com.erpcore.domain.identity.Membership;
import br.com.erpcore.domain.identity.RefreshToken;
import br.com.erpcore.domain.identity.RefreshTokenId;
import br.com.erpcore.domain.identity.SessionPolicy;
import br.com.erpcore.domain.identity.TokenFamilyId;
import br.com.erpcore.domain.identity.TokenHash;
import java.time.Instant;

/** Emite o par de tokens de uma sessão, no login e a cada renovação. Precisa rodar dentro de uma transação. */
public final class SessionIssuer {

    private final RefreshTokenRepository refreshTokens;
    private final RefreshTokenCodec refreshCodec;
    private final AccessTokenIssuer accessTokens;
    private final IdGenerator ids;
    private final SessionPolicy policy;

    public SessionIssuer(RefreshTokenRepository refreshTokens, RefreshTokenCodec refreshCodec,
                         AccessTokenIssuer accessTokens, IdGenerator ids, SessionPolicy policy) {
        this.refreshTokens = refreshTokens;
        this.refreshCodec = refreshCodec;
        this.accessTokens = accessTokens;
        this.ids = ids;
        this.policy = policy;
    }

    /** Sessão nova (login): começa uma família de refresh tokens. */
    public SessionTokens startSession(Membership membership, Instant now) {
        NewRefreshSecret secret = refreshCodec.generate();
        RefreshToken token = RefreshToken.first(new RefreshTokenId(ids.newId()), membership.userId(),
                membership.companyId(), new TokenFamilyId(ids.newId()), new TokenHash(secret.hash()), now, policy);
        refreshTokens.save(token);
        return tokensFor(membership, token, secret);
    }

    /** Renovação: o token atual deixa de valer e um novo o substitui na mesma sessão. */
    public SessionTokens continueSession(RefreshToken current, Membership membership, Instant now) {
        NewRefreshSecret secret = refreshCodec.generate();
        RefreshToken next = current.rotateTo(new RefreshTokenId(ids.newId()), new TokenHash(secret.hash()), now, policy);
        refreshTokens.save(current);
        refreshTokens.save(next);
        return tokensFor(membership, next, secret);
    }

    private SessionTokens tokensFor(Membership membership, RefreshToken token, NewRefreshSecret secret) {
        IssuedAccessToken access = accessTokens.issue(new AccessTokenSubject(
                membership.userId().value(), membership.companyId().value(), membership.role().name()));
        return new SessionTokens(access.value(), access.expiresAt(), secret.value(), token.expiresAt());
    }
}
