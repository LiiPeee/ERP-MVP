package br.com.erpcore.application.contract;

import br.com.erpcore.application.identity.port.out.RefreshTokenRepository;
import br.com.erpcore.application.shared.port.out.AccessScope;
import br.com.erpcore.application.support.InMemoryRefreshTokenRepository;
import br.com.erpcore.application.support.InMemoryUnitOfWork;
import br.com.erpcore.domain.company.Company;
import br.com.erpcore.domain.identity.User;
import java.util.function.Supplier;

class InMemoryRefreshTokenRepositoryContractTest extends RefreshTokenRepositoryContract {

    private final InMemoryUnitOfWork unitOfWork = new InMemoryUnitOfWork();
    private final InMemoryRefreshTokenRepository repository = new InMemoryRefreshTokenRepository(unitOfWork);

    @Override
    protected RefreshTokenRepository repository() {
        return repository;
    }

    @Override
    protected void givenUserAndCompanyExist(User user, Company company) {
        // O fake não tem chaves estrangeiras: não há nada a preparar.
    }

    @Override
    protected <T> T inTransaction(Supplier<T> work) {
        return unitOfWork.execute(new AccessScope.Anonymous(), work);
    }
}
