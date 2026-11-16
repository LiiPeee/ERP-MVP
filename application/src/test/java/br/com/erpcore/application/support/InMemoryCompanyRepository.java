package br.com.erpcore.application.support;

import br.com.erpcore.application.company.port.out.CompanyRepository;
import br.com.erpcore.domain.company.Company;
import br.com.erpcore.domain.company.CompanyId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemoryCompanyRepository implements CompanyRepository {

    private final InMemoryUnitOfWork unitOfWork;
    private final Map<CompanyId, Company> storage = new LinkedHashMap<>();

    public InMemoryCompanyRepository(InMemoryUnitOfWork unitOfWork) {
        this.unitOfWork = unitOfWork;
    }

    @Override
    public void save(Company company) {
        unitOfWork.requireActive();
        storage.put(company.id(), company);
    }

    @Override
    public Optional<Company> findById(CompanyId id) {
        unitOfWork.requireActive();
        return Optional.ofNullable(storage.get(id));
    }

    public List<Company> all() {
        return List.copyOf(storage.values());
    }
}
