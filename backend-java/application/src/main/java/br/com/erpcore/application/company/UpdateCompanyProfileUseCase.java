package br.com.erpcore.application.company;

import br.com.erpcore.application.audit.AuditRecorder;
import br.com.erpcore.application.company.port.out.CompanyProfileRepository;
import br.com.erpcore.application.identity.OwnerGuard;
import br.com.erpcore.application.shared.port.out.AccessScope;
import br.com.erpcore.application.shared.port.out.UnitOfWork;
import br.com.erpcore.domain.audit.AuditEventType;
import br.com.erpcore.domain.company.CompanyId;
import br.com.erpcore.domain.company.CompanyProfile;
import br.com.erpcore.domain.contact.PhoneNumber;
import br.com.erpcore.domain.identity.UserId;
import java.util.Map;

public final class UpdateCompanyProfileUseCase {

    private final UnitOfWork unitOfWork;
    private final CompanyProfileRepository profiles;
    private final OwnerGuard guard;
    private final AuditRecorder audit;

    public UpdateCompanyProfileUseCase(UnitOfWork unitOfWork, CompanyProfileRepository profiles, OwnerGuard guard,
                                       AuditRecorder audit) {
        this.unitOfWork = unitOfWork;
        this.profiles = profiles;
        this.guard = guard;
        this.audit = audit;
    }

    public CompanyProfileView execute(UpdateCompanyProfileCommand command) {
        CompanyId companyId = new CompanyId(command.principal().companyId());
        UserId userId = new UserId(command.principal().userId());
        return unitOfWork.execute(new AccessScope.Tenant(userId, companyId), () -> {
            guard.require(command.principal());
            PhoneNumber phone = command.businessPhone() == null || command.businessPhone().isBlank()
                    ? null : PhoneNumber.of(command.businessPhone());
            CompanyProfile profile = CompanyProfile.of(companyId, command.displayName(), phone, command.address(),
                    command.openingHours(), command.defaultValidityDays(), command.signatureMessage());
            profiles.save(profile);
            audit.success(AuditEventType.COMPANY_PROFILE_UPDATED, companyId, userId, command.correlationId(), Map.of());
            return CompanyProfileView.from(profile);
        });
    }
}
