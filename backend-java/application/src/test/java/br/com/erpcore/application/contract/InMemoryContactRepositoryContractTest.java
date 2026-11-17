package br.com.erpcore.application.contract;

import br.com.erpcore.application.contact.port.out.ContactRepository;
import br.com.erpcore.application.shared.port.out.AccessScope;
import br.com.erpcore.application.support.InMemoryContactRepository;
import br.com.erpcore.application.support.InMemoryUnitOfWork;
import br.com.erpcore.domain.company.Company;
import br.com.erpcore.domain.contact.Contact;
import br.com.erpcore.domain.quote.Quote;
import br.com.erpcore.domain.vehicle.Vehicle;
import java.util.function.Supplier;

class InMemoryContactRepositoryContractTest extends ContactRepositoryContract {

    private final InMemoryUnitOfWork unitOfWork = new InMemoryUnitOfWork();
    private final InMemoryContactRepository repository = new InMemoryContactRepository(unitOfWork);

    @Override
    protected ContactRepository repository() {
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
