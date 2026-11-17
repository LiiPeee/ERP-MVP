package br.com.erpcore.application.contract;

import br.com.erpcore.application.quote.port.out.QuoteRepository;
import br.com.erpcore.application.shared.port.out.AccessScope;
import br.com.erpcore.application.support.InMemoryQuoteRepository;
import br.com.erpcore.application.support.InMemoryUnitOfWork;
import br.com.erpcore.domain.company.Company;
import br.com.erpcore.domain.contact.Contact;
import br.com.erpcore.domain.quote.Quote;
import br.com.erpcore.domain.vehicle.Vehicle;
import java.util.function.Supplier;

class InMemoryQuoteRepositoryContractTest extends QuoteRepositoryContract {

    private final InMemoryUnitOfWork unitOfWork = new InMemoryUnitOfWork();
    private final InMemoryQuoteRepository repository = new InMemoryQuoteRepository(unitOfWork);

    @Override
    protected QuoteRepository repository() {
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
