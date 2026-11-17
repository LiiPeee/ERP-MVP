package br.com.erpcore.adapter.persistence.contract;

import br.com.erpcore.adapter.persistence.support.PostgresTestDatabase;
import br.com.erpcore.adapter.persistence.support.SuperuserPostgresTest;
import br.com.erpcore.application.company.port.out.CompanyRepository;
import br.com.erpcore.application.contract.MembershipRepositoryContract;
import br.com.erpcore.application.identity.port.out.MembershipRepository;
import br.com.erpcore.application.identity.port.out.UserRepository;
import br.com.erpcore.application.shared.port.out.AccessScope;
import br.com.erpcore.application.shared.port.out.UnitOfWork;
import br.com.erpcore.domain.company.Company;
import br.com.erpcore.domain.identity.User;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

@SuperuserPostgresTest
class PostgresMembershipRepositoryContractIT extends MembershipRepositoryContract {

    @Autowired
    private MembershipRepository repository;

    @Autowired
    private UserRepository users;

    @Autowired
    private CompanyRepository companies;

    @Autowired
    private UnitOfWork unitOfWork;

    @BeforeEach
    void startFromAnEmptyDatabase() {
        PostgresTestDatabase.truncateAll();
    }

    @Override
    protected MembershipRepository repository() {
        return repository;
    }

    @Override
    protected void givenUserAndCompaniesExist(User user, Company... existing) {
        inTransaction(() -> {
            users.save(user);
            for (Company company : existing) {
                companies.save(company);
            }
            return null;
        });
    }

    @Override
    protected <T> T inTransaction(Supplier<T> work) {
        return unitOfWork.execute(new AccessScope.Anonymous(), work);
    }
}
