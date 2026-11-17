package br.com.erpcore.application.contract;

import br.com.erpcore.domain.company.Company;
import br.com.erpcore.domain.contact.Contact;
import br.com.erpcore.domain.quote.Quote;
import br.com.erpcore.domain.vehicle.Vehicle;
import java.util.function.Supplier;

/**
 * Base dos contratos de contato, veículo e orçamento. Os ganchos deixam de pé o que as chaves estrangeiras do
 * banco real exigem; o fake em memória não tem chaves estrangeiras e não faz nada neles.
 */
public abstract class BusinessContract {

    protected abstract <T> T inTransaction(Supplier<T> work);

    protected abstract void givenCompaniesExist(Company... companies);

    protected abstract void givenContactsExist(Contact... contacts);

    protected abstract void givenVehiclesExist(Vehicle... vehicles);

    protected abstract void givenQuotesExist(Quote... quotes);

    protected final void inTransaction(Runnable work) {
        inTransaction(() -> {
            work.run();
            return null;
        });
    }
}
