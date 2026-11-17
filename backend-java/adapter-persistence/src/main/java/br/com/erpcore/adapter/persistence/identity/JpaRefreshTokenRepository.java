package br.com.erpcore.adapter.persistence.identity;

import br.com.erpcore.adapter.persistence.tenancy.ScopeVariables;
import br.com.erpcore.application.identity.port.out.RefreshTokenRepository;
import br.com.erpcore.domain.company.CompanyId;
import br.com.erpcore.domain.identity.RefreshToken;
import br.com.erpcore.domain.identity.RefreshTokenId;
import br.com.erpcore.domain.identity.TokenFamilyId;
import br.com.erpcore.domain.identity.TokenHash;
import br.com.erpcore.domain.identity.UserId;
import java.time.Instant;
import java.util.Optional;

public final class JpaRefreshTokenRepository implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository jpa;
    private final ScopeVariables scopeVariables;

    public JpaRefreshTokenRepository(RefreshTokenJpaRepository jpa, ScopeVariables scopeVariables) {
        this.jpa = jpa;
        this.scopeVariables = scopeVariables;
    }

    @Override
    public void save(RefreshToken token) {
        jpa.save(new RefreshTokenJpaEntity(token.id().value(), token.userId().value(), token.companyId().value(),
                token.familyId().value(), token.tokenHash().value(), token.issuedAt(), token.expiresAt(),
                token.familyStartedAt(), token.revokedAt().orElse(null)));
    }

    /**
     * Procura pelo hash e trava a linha. Ao achar, grava na transação a sessão (família) do token: é ela que
     * dá à transação o direito de ver, revogar e criar os outros tokens da mesma sessão, e só ela.
     */
    @Override
    public Optional<RefreshToken> findByHash(TokenHash hash) {
        Optional<RefreshTokenJpaEntity> found = jpa.findByTokenHashForUpdate(hash.value());
        found.ifPresent(entity -> scopeVariables.rememberTokenFamily(entity.getFamilyId()));
        return found.map(JpaRefreshTokenRepository::toDomain);
    }

    @Override
    public void revokeFamily(TokenFamilyId familyId, Instant now) {
        jpa.revokeFamily(familyId.value(), now);
    }

    private static RefreshToken toDomain(RefreshTokenJpaEntity entity) {
        return RefreshToken.restore(new RefreshTokenId(entity.getId()), new UserId(entity.getUserId()),
                new CompanyId(entity.getCompanyId()), new TokenFamilyId(entity.getFamilyId()),
                new TokenHash(entity.getTokenHash()), entity.getIssuedAt(), entity.getExpiresAt(),
                entity.getFamilyStartedAt(), entity.getRevokedAt());
    }
}
