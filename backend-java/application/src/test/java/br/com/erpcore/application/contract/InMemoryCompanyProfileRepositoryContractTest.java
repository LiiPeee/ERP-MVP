package br.com.erpcore.application.contract;

import br.com.erpcore.application.profile.port.out.CompanyProfileRepository;
import br.com.erpcore.application.shared.port.out.AccessScope;
import br.com.erpcore.application.support.InMemoryCompanyProfileRepository;
import br.com.erpcore.application.support.InMemoryUnitOfWork;
import br.com.erpcore.domain.company.Company;
import br.com.erpcore.domain.contact.Contact;
import br.com.erpcore.domain.quote.Quote;
import br.com.erpcore.domain.vehicle.Vehicle;
import java.util.function.Supplier;

class InMemoryCompanyProfileRepositoryContractTest extends CompanyProfileRepositoryContract {

    private final InMemoryUnitOfWork unitOfWork = new InMemoryUnitOfWork();
    private final InMemoryCompanyProfileRepository repository = new InMemoryCompanyProfileRepository(unitOfWork);

    @Override
    protected CompanyProfileRepository repository() {
        return repository;
    }

    @Override
    protected <T> T inTransaction(Supplier<T> work) {
        return unitOfWork.execute(new AccessScope.Anonymous(), work);
    }

    // O fake não tem chaves estrangeiras: não há nada a preparar.
    @Override
    protected void givenCompaniesExist(Company... companies) {
    }

    @Override
    protected void givenContactsExist(Contact... contacts) {
    }

    @Override
    protected void givenVehiclesExist(Vehicle... vehicles) {
    }

    @Override
    protected void givenQuotesExist(Quote... quotes) {
    }
}
