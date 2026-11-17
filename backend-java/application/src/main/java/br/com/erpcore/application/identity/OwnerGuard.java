package br.com.erpcore.application.identity;

import br.com.erpcore.application.identity.port.out.MembershipRepository;
import br.com.erpcore.domain.company.CompanyId;
import br.com.erpcore.domain.identity.AccessDeniedException;
import br.com.erpcore.domain.identity.Membership;
import br.com.erpcore.domain.identity.Role;
import br.com.erpcore.domain.identity.UserId;
import java.util.Optional;

/**
 * A autorização das funções do dono, feita no caso de uso e não só no controller: um token válido não basta, a
 * pessoa precisa ainda ser dona da empresa. Precisa ser chamado dentro da transação do caso de uso.
 */
public final class OwnerGuard {

    private final MembershipRepository memberships;

    public OwnerGuard(MembershipRepository memberships) {
        this.memberships = memberships;
    }

    public void require(AuthenticatedPrincipal principal) {
        Optional<Membership> membership = memberships.find(new CompanyId(principal.companyId()), new UserId(principal.userId()));
        if (membership.isEmpty() || membership.get().role() != Role.OWNER) {
            throw new AccessDeniedException();
        }
    }
}
