package br.com.erpcore.application.identity;

import br.com.erpcore.application.audit.AuditRecorder;
import br.com.erpcore.application.company.port.out.CompanyRepository;
import br.com.erpcore.application.identity.port.out.MembershipRepository;
import br.com.erpcore.application.identity.port.out.UserRepository;
import br.com.erpcore.application.shared.port.out.AccessScope;
import br.com.erpcore.application.shared.port.out.UnitOfWork;
import br.com.erpcore.domain.audit.AuditEventType;
import br.com.erpcore.domain.company.Company;
import br.com.erpcore.domain.company.CompanyId;
import br.com.erpcore.domain.identity.AccessDeniedException;
import br.com.erpcore.domain.identity.Membership;
import br.com.erpcore.domain.identity.User;
import br.com.erpcore.domain.identity.UserId;
import java.util.Optional;

/**
 * Devolve quem é o usuário autenticado, a empresa e o papel. A autorização é feita aqui, no caso de uso:
 * mesmo com um token válido, só quem ainda é membro da empresa enxerga o contexto dela.
 */
public final class GetCurrentContextUseCase {

    private static final String REASON_NOT_A_MEMBER = "NOT_A_MEMBER";

    private final UnitOfWork unitOfWork;
    private final UserRepository users;
    private final CompanyRepository companies;
    private final MembershipRepository memberships;
    private final AuditRecorder audit;

    public GetCurrentContextUseCase(UnitOfWork unitOfWork, UserRepository users, CompanyRepository companies,
                                    MembershipRepository memberships, AuditRecorder audit) {
        this.unitOfWork = unitOfWork;
        this.users = users;
        this.companies = companies;
        this.memberships = memberships;
        this.audit = audit;
    }

    public CurrentContext execute(GetCurrentContextQuery query) {
        UserId userId = new UserId(query.principal().userId());
        CompanyId companyId = new CompanyId(query.principal().companyId());

        Outcome outcome = unitOfWork.execute(new AccessScope.Tenant(userId, companyId),
                () -> read(userId, companyId, query.correlationId()));

        return switch (outcome) {
            case Outcome.Allowed allowed -> allowed.context();
            case Outcome.Denied denied -> throw new AccessDeniedException();
        };
    }

    private Outcome read(UserId userId, CompanyId companyId, String correlationId) {
        Optional<Membership> membership = memberships.find(companyId, userId);
        Optional<User> user = users.findById(userId);
        Optional<Company> company = companies.findById(companyId);
        if (membership.isEmpty() || user.isEmpty() || company.isEmpty()) {
            audit.failure(AuditEventType.CONTEXT_VIEWED, companyId, userId, correlationId, REASON_NOT_A_MEMBER);
            return new Outcome.Denied();
        }

        audit.success(AuditEventType.CONTEXT_VIEWED, companyId, userId, correlationId);
        return new Outcome.Allowed(new CurrentContext(userId.value(), user.get().email().value(),
                user.get().displayName().value(), companyId.value(), company.get().name().value(),
                membership.get().role().name()));
    }

    private sealed interface Outcome {

        record Allowed(CurrentContext context) implements Outcome {
        }

        record Denied() implements Outcome {
        }
    }
}
