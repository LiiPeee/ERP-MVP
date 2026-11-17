package br.com.erpcore.application.support;

import br.com.erpcore.application.company.port.out.CompanyProfileRepository;
import br.com.erpcore.domain.company.CompanyId;
import br.com.erpcore.domain.company.CompanyProfile;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class InMemoryCompanyProfileRepository implements CompanyProfileRepository {

    private final InMemoryUnitOfWork unitOfWork;
    private final Map<CompanyId, CompanyProfile> storage = new LinkedHashMap<>();

    public InMemoryCompanyProfileRepository(InMemoryUnitOfWork unitOfWork) {
        this.unitOfWork = unitOfWork;
    }

    @Override
    public void save(CompanyProfile profile) {
        unitOfWork.requireActive();
        storage.put(profile.companyId(), profile);
    }

    @Override
    public Optional<CompanyProfile> find(CompanyId companyId) {
        unitOfWork.requireActive();
        return Optional.ofNullable(storage.get(companyId));
    }
}
