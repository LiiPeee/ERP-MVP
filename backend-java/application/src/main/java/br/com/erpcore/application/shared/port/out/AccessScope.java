package br.com.erpcore.application.shared.port.out;

import br.com.erpcore.domain.company.CompanyId;
import br.com.erpcore.domain.identity.Email;
import br.com.erpcore.domain.identity.TokenHash;
import br.com.erpcore.domain.identity.UserId;

/**
 * O que uma transação pode enxergar. {@link Tenant} é o caso comum e vem sempre do token autenticado. Os
 * demais existem para o que acontece antes de haver empresa autenticada, e cada um abre só o mínimo.
 */
public sealed interface AccessScope {

    /** Sem identidade: só permite gravar auditoria. */
    record Anonymous() implements AccessScope {
    }

    /** Cadastro de uma empresa nova: enxerga só a conta com este e-mail e grava apenas nesta empresa. */
    record Registration(CompanyId companyId, Email email) implements AccessScope {
    }

    /** Login: enxerga só a conta com este e-mail e o que pertence a ela. */
    record CredentialLookup(Email email) implements AccessScope {
    }

    /** Renovação ou saída: enxerga só o refresh token com este hash e o que pertence a ele. */
    record RefreshTokenLookup(TokenHash tokenHash) implements AccessScope {
    }

    /** Usuário autenticado agindo dentro da própria empresa. */
    record Tenant(UserId userId, CompanyId companyId) implements AccessScope {
    }
}
