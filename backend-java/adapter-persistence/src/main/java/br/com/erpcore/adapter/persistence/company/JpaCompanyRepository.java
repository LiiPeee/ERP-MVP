package br.com.erpcore.adapter.persistence.company;

import br.com.erpcore.application.company.port.out.CompanyRepository;
import br.com.erpcore.domain.company.Company;
import br.com.erpcore.domain.company.CompanyId;
import br.com.erpcore.domain.company.CompanyName;
import java.util.Optional;

public final class JpaCompanyRepository implements CompanyRepository {

    private final CompanyJpaRepository jpa;

    public JpaCompanyRepository(CompanyJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void save(Company company) {
        jpa.save(new CompanyJpaEntity(company.id().value(), company.name().value(), company.createdAt()));
    }

    @Override
    public Optional<Company> findById(CompanyId id) {
        return jpa.findById(id.value()).map(entity ->
                new Company(new CompanyId(entity.getId()), CompanyName.of(entity.getName()), entity.getCreatedAt()));
    }
}
