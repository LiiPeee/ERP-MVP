package br.com.erpcore.adapter.persistence.contract;

import br.com.erpcore.adapter.persistence.support.PostgresTestDatabase;
import br.com.erpcore.adapter.persistence.support.SuperuserPostgresTest;
import br.com.erpcore.application.company.port.out.CompanyRepository;
import br.com.erpcore.application.contact.port.out.ContactRepository;
import br.com.erpcore.application.contract.QuoteLinkRepositoryContract;
import br.com.erpcore.application.profile.port.out.CompanyProfileRepository;
import br.com.erpcore.application.quote.port.out.QuoteLinkRepository;
import br.com.erpcore.application.quote.port.out.QuoteRepository;
import br.com.erpcore.application.shared.port.out.AccessScope;
import br.com.erpcore.application.shared.port.out.UnitOfWork;
import br.com.erpcore.application.vehicle.port.out.VehicleRepository;
import br.com.erpcore.domain.company.Company;
import br.com.erpcore.domain.contact.Contact;
import br.com.erpcore.domain.quote.Quote;
import br.com.erpcore.domain.vehicle.Vehicle;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

@SuperuserPostgresTest
class PostgresQuoteLinkRepositoryContractIT extends QuoteLinkRepositoryContract {

    @Autowired
    private QuoteLinkRepository repository;
    @Autowired
    private CompanyRepository companies;
    @Autowired
    private ContactRepository contacts;
    @Autowired
    private VehicleRepository vehicles;
    @Autowired
    private QuoteRepository quotes;
    @Autowired
    private UnitOfWork unitOfWork;

    @BeforeEach
    void startFromAnEmptyDatabase() {
        PostgresTestDatabase.truncateAll();
    }

    @Override
    protected QuoteLinkRepository repository() {
        return repository;
    }

    @Override
    protected <T> T inTransaction(Supplier<T> work) {
        return unitOfWork.execute(new AccessScope.Anonymous(), work);
    }

    @Override
    protected void givenCompaniesExist(Company... existing) {
        inTransaction(() -> {
            for (Company company : existing) {
                companies.save(company);
            }
            return null;
        });
    }

    @Override
    protected void givenContactsExist(Contact... existing) {
        inTransaction(() -> {
            for (Contact contact : existing) {
                contacts.save(contact);
            }
            return null;
        });
    }

    @Override
    protected void givenVehiclesExist(Vehicle... existing) {
        inTransaction(() -> {
            for (Vehicle vehicle : existing) {
                vehicles.save(vehicle);
            }
            return null;
        });
    }

    @Override
    protected void givenQuotesExist(Quote... existing) {
        inTransaction(() -> {
            for (Quote quote : existing) {
                quotes.save(quote);
            }
            return null;
        });
    }
}
