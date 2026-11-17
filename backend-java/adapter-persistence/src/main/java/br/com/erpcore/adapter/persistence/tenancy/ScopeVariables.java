package br.com.erpcore.adapter.persistence.tenancy;

import br.com.erpcore.application.shared.port.out.AccessScope;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Único lugar que escreve as variáveis de sessão que as políticas de Row Level Security leem. Todas valem só
 * até o fim da transação ({@code set_config(..., true)}), então nada vaza para o próximo uso da conexão.
 */
public final class ScopeVariables {

    private static final String NONE = "";

    private static final String APPLY_SQL = """
            SELECT set_config('app.scope', :scope, true),
                   set_config('app.company_id', :companyId, true),
                   set_config('app.user_id', :userId, true),
                   set_config('app.email', :email, true),
                   set_config('app.token_hash', :tokenHash, true),
                   set_config('app.family_id', '', true)
            """;

    private static final String REMEMBER_FAMILY_SQL = "SELECT set_config('app.family_id', :familyId, true)";

    private final JdbcClient jdbc;

    public ScopeVariables(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void apply(AccessScope scope) {
        Values values = switch (scope) {
            case AccessScope.Anonymous anonymous -> new Values("anonymous", NONE, NONE, NONE, NONE);
            case AccessScope.Registration registration -> new Values("registration",
                    registration.companyId().value().toString(), NONE, registration.email().value(), NONE);
            case AccessScope.CredentialLookup lookup -> new Values("credential_lookup", NONE, NONE,
                    lookup.email().value(), NONE);
            case AccessScope.RefreshTokenLookup lookup -> new Values("refresh_token_lookup", NONE, NONE, NONE,
                    lookup.tokenHash().value());
            case AccessScope.Tenant tenant -> new Values("tenant", tenant.companyId().value().toString(),
                    tenant.userId().value().toString(), NONE, NONE);
        };
        jdbc.sql(APPLY_SQL)
                .param("scope", values.scope())
                .param("companyId", values.companyId())
                .param("userId", values.userId())
                .param("email", values.email())
                .param("tokenHash", values.tokenHash())
                .query().listOfRows();
    }

    /** Chamado depois de achar um refresh token: a sessão dele passa a ser visível nesta transação. */
    public void rememberTokenFamily(UUID familyId) {
        jdbc.sql(REMEMBER_FAMILY_SQL).param("familyId", familyId.toString()).query().listOfRows();
    }

    private record Values(String scope, String companyId, String userId, String email, String tokenHash) {
    }
}
