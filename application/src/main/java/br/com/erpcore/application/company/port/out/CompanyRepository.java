package br.com.erpcore.application.company.port.out;

import br.com.erpcore.domain.company.Company;
import br.com.erpcore.domain.company.CompanyId;
import java.util.Optional;

public interface CompanyRepository {

    void save(Company company);

    Optional<Company> findById(CompanyId id);
}
