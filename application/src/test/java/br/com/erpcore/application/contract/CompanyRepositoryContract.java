package br.com.erpcore.application.contract;

import static br.com.erpcore.application.contract.ContractData.company;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.erpcore.application.company.port.out.CompanyRepository;
import br.com.erpcore.domain.company.Company;
import br.com.erpcore.domain.company.CompanyId;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

/** O mesmo contrato roda contra o fake em memória e contra o adaptador de PostgreSQL. */
public abstract class CompanyRepositoryContract {

    protected abstract CompanyRepository repository();

    protected abstract <T> T inTransaction(Supplier<T> work);

    @Test
    void givenASavedCompany_whenFindingItById_thenEveryFieldComesBack() {
        Company saved = company(1);
        inTransaction(() -> {
            repository().save(saved);
            return null;
        });

        Company found = inTransaction(() -> repository().findById(saved.id())).orElseThrow();

        assertThat(found.id()).isEqualTo(saved.id());
        assertThat(found.name()).isEqualTo(saved.name());
        assertThat(found.createdAt()).isEqualTo(saved.createdAt());
    }

    @Test
    void givenNoSuchCompany_whenFindingItById_thenTheResultIsEmpty() {
        assertThat(inTransaction(() -> repository().findById(new CompanyId(UUID.randomUUID())))).isEmpty();
    }
}
