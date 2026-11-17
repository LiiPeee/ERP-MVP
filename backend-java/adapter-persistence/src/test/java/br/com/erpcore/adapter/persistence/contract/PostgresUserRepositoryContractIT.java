package br.com.erpcore.adapter.persistence.contract;

import br.com.erpcore.adapter.persistence.support.PostgresTestDatabase;
import br.com.erpcore.adapter.persistence.support.SuperuserPostgresTest;
import br.com.erpcore.application.contract.UserRepositoryContract;
import br.com.erpcore.application.identity.port.out.UserRepository;
import br.com.erpcore.application.shared.port.out.AccessScope;
import br.com.erpcore.application.shared.port.out.UnitOfWork;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

@SuperuserPostgresTest
class PostgresUserRepositoryContractIT extends UserRepositoryContract {

    @Autowired
    private UserRepository repository;

    @Autowired
    private UnitOfWork unitOfWork;

    @BeforeEach
    void startFromAnEmptyDatabase() {
        PostgresTestDatabase.truncateAll();
    }

    @Override
    protected UserRepository repository() {
        return repository;
    }

    @Override
    protected <T> T inTransaction(Supplier<T> work) {
        return unitOfWork.execute(new AccessScope.Anonymous(), work);
    }
}
