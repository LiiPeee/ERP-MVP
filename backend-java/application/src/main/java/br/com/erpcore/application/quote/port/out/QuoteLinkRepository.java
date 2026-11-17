package br.com.erpcore.application.quote.port.out;

import br.com.erpcore.domain.identity.TokenHash;
import br.com.erpcore.domain.quote.QuoteLink;
import java.util.Optional;

public interface QuoteLinkRepository {

    /** Insere o link ou atualiza o que já existe com o mesmo hash. */
    void save(QuoteLink link);

    /** Só o hash do token é guardado; o token em si nunca. */
    Optional<QuoteLink> findByHash(TokenHash hash);
}
