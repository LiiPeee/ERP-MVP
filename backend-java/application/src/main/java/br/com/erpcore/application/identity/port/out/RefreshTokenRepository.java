package br.com.erpcore.application.identity.port.out;

import br.com.erpcore.domain.identity.RefreshToken;
import br.com.erpcore.domain.identity.TokenFamilyId;
import br.com.erpcore.domain.identity.TokenHash;
import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository {

    /** Insere o token ou atualiza o que já existe com o mesmo id. */
    void save(RefreshToken token);

    Optional<RefreshToken> findByHash(TokenHash hash);

    /** Revoga, em uma só operação, todos os tokens ainda válidos da sessão. */
    void revokeFamily(TokenFamilyId familyId, Instant now);
}
