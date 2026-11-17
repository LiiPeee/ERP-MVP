package br.com.erpcore.application.contract;

import br.com.erpcore.application.company.port.out.CompanyRepository;
import br.com.erpcore.application.shared.port.out.AccessScope;
import br.com.erpcore.application.support.InMemoryCompanyRepository;
import br.com.erpcore.application.support.InMemoryUnitOfWork;
import java.util.function.Supplier;

class InMemoryCompanyRepositoryContractTest extends CompanyRepositoryContract {

    private final InMemoryUnitOfWork unitOfWork = new InMemoryUnitOfWork();
    private final InMemoryCompanyRepository repository = new InMemoryCompanyRepository(unitOfWork);

    @Override
    protected CompanyRepository repository() {
        return repository;
    }

    @Override
    protected <T> T inTransaction(Supplier<T> work) {
        return unitOfWork.execute(new AccessScope.Anonymous(), work);
    }
}
