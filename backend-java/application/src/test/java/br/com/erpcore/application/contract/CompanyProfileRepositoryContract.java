package br.com.erpcore.application.contract;

import static br.com.erpcore.application.contract.BusinessContractData.profile;
import static br.com.erpcore.application.contract.ContractData.company;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.erpcore.application.profile.port.out.CompanyProfileRepository;
import br.com.erpcore.domain.company.Company;
import br.com.erpcore.domain.company.CompanyProfile;
import org.junit.jupiter.api.Test;

/** O mesmo contrato roda contra o fake em memória e contra o adaptador de PostgreSQL. */
public abstract class CompanyProfileRepositoryContract extends BusinessContract {

    private final Company company = company(1);
    private final Company otherCompany = company(2);

    protected abstract CompanyProfileRepository repository();

    @Test
    void givenASavedProfile_whenFindingIt_thenEveryFieldComesBack() {
        givenCompaniesExist(company);
        CompanyProfile saved = profile(company, 10);
        inTransaction(() -> repository().save(saved));

        CompanyProfile found = inTransaction(() -> repository().find(company.id())).orElseThrow();

        assertThat(found).isEqualTo(saved);
    }

    @Test
    void givenAProfileSavedTwice_whenFindingIt_thenTheLastOneWins() {
        givenCompaniesExist(company);
        inTransaction(() -> repository().save(profile(company, 10)));
        inTransaction(() -> repository().save(profile(company, 20)));

        assertThat(inTransaction(() -> repository().find(company.id())).orElseThrow().defaultValidityDays()).isEqualTo(20);
    }

    @Test
    void givenACompanyWithoutProfile_whenFindingIt_thenTheResultIsEmpty() {
        givenCompaniesExist(company, otherCompany);
        inTransaction(() -> repository().save(profile(company, 10)));

        assertThat(inTransaction(() -> repository().find(otherCompany.id()))).isEmpty();
    }
}
