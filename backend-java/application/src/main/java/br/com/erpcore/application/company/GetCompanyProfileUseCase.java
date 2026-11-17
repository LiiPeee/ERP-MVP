package br.com.erpcore.application.company;

import br.com.erpcore.application.company.port.out.CompanyProfileRepository;
import br.com.erpcore.application.company.port.out.CompanyRepository;
import br.com.erpcore.application.identity.OwnerGuard;
import br.com.erpcore.application.shared.port.out.AccessScope;
import br.com.erpcore.application.shared.port.out.UnitOfWork;
import br.com.erpcore.domain.company.CompanyId;
import br.com.erpcore.domain.company.CompanyProfile;
import br.com.erpcore.domain.identity.AccessDeniedException;
import br.com.erpcore.domain.identity.UserId;

/** O perfil da oficina; enquanto ela não preencher nada, vale o padrão (nome da empresa e 7 dias de validade). */
public final class GetCompanyProfileUseCase {

    private final UnitOfWork unitOfWork;
    private final CompanyProfileRepository profiles;
    private final CompanyRepository companies;
    private final OwnerGuard guard;

    public GetCompanyProfileUseCase(UnitOfWork unitOfWork, CompanyProfileRepository profiles, CompanyRepository companies,
                                    OwnerGuard guard) {
        this.unitOfWork = unitOfWork;
        this.profiles = profiles;
        this.companies = companies;
        this.guard = guard;
    }

    public CompanyProfileView execute(GetCompanyProfileQuery query) {
        CompanyId companyId = new CompanyId(query.principal().companyId());
        UserId userId = new UserId(query.principal().userId());
        return unitOfWork.execute(new AccessScope.Tenant(userId, companyId), () -> {
            guard.require(query.principal());
            CompanyProfile profile = profiles.find(companyId).orElseGet(() -> CompanyProfile.defaultFor(
                    companies.findById(companyId).orElseThrow(AccessDeniedException::new)));
            return CompanyProfileView.from(profile);
        });
    }
}
