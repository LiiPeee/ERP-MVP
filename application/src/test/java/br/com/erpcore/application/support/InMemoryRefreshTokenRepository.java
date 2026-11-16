package br.com.erpcore.application.support;

import br.com.erpcore.application.identity.port.out.RefreshTokenRepository;
import br.com.erpcore.domain.identity.RefreshToken;
import br.com.erpcore.domain.identity.RefreshTokenId;
import br.com.erpcore.domain.identity.TokenFamilyId;
import br.com.erpcore.domain.identity.TokenHash;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Guarda e devolve cópias, como um banco: alterar o objeto lido não muda nada sem {@code save}. */
public final class InMemoryRefreshTokenRepository implements RefreshTokenRepository {

    private final InMemoryUnitOfWork unitOfWork;
    private final Map<RefreshTokenId, RefreshToken> storage = new LinkedHashMap<>();

    public InMemoryRefreshTokenRepository(InMemoryUnitOfWork unitOfWork) {
        this.unitOfWork = unitOfWork;
    }

    @Override
    public void save(RefreshToken token) {
        unitOfWork.requireActive();
        storage.put(token.id(), copy(token));
    }

    @Override
    public Optional<RefreshToken> findByHash(TokenHash hash) {
        unitOfWork.requireActive();
        return storage.values().stream().filter(token -> token.tokenHash().equals(hash)).findFirst().map(this::copy);
    }

    @Override
    public void revokeFamily(TokenFamilyId familyId, Instant now) {
        unitOfWork.requireActive();
        storage.values().stream().filter(token -> token.familyId().equals(familyId)).forEach(token -> token.revoke(now));
    }

    public List<RefreshToken> all() {
        return storage.values().stream().map(this::copy).toList();
    }

    private RefreshToken copy(RefreshToken token) {
        return RefreshToken.restore(token.id(), token.userId(), token.companyId(), token.familyId(), token.tokenHash(),
                token.issuedAt(), token.expiresAt(), token.familyStartedAt(), token.revokedAt().orElse(null));
    }
}
