package br.com.erpcore.application.contract;

import br.com.erpcore.application.identity.port.out.MembershipRepository;
import br.com.erpcore.application.shared.port.out.AccessScope;
import br.com.erpcore.application.support.InMemoryMembershipRepository;
import br.com.erpcore.application.support.InMemoryUnitOfWork;
import br.com.erpcore.domain.company.Company;
import br.com.erpcore.domain.identity.User;
import java.util.function.Supplier;

class InMemoryMembershipRepositoryContractTest extends MembershipRepositoryContract {

    private final InMemoryUnitOfWork unitOfWork = new InMemoryUnitOfWork();
    private final InMemoryMembershipRepository repository = new InMemoryMembershipRepository(unitOfWork);

    @Override
    protected MembershipRepository repository() {
        return repository;
    }

    @Override
    protected void givenUserAndCompaniesExist(User user, Company... companies) {
        // O fake não tem chaves estrangeiras: não há nada a preparar.
    }

    @Override
    protected <T> T inTransaction(Supplier<T> work) {
        return unitOfWork.execute(new AccessScope.Anonymous(), work);
    }
}
