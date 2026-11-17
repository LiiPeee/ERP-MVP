package br.com.erpcore.application.contract;

import br.com.erpcore.application.identity.port.out.UserRepository;
import br.com.erpcore.application.shared.port.out.AccessScope;
import br.com.erpcore.application.support.InMemoryUnitOfWork;
import br.com.erpcore.application.support.InMemoryUserRepository;
import java.util.function.Supplier;

class InMemoryUserRepositoryContractTest extends UserRepositoryContract {

    private final InMemoryUnitOfWork unitOfWork = new InMemoryUnitOfWork();
    private final InMemoryUserRepository repository = new InMemoryUserRepository(unitOfWork);

    @Override
    protected UserRepository repository() {
        return repository;
    }

    @Override
    protected <T> T inTransaction(Supplier<T> work) {
        return unitOfWork.execute(new AccessScope.Anonymous(), work);
    }
}
