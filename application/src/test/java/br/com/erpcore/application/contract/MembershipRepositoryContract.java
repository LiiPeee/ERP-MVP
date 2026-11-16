package br.com.erpcore.application.contract;

import static br.com.erpcore.application.contract.ContractData.company;
import static br.com.erpcore.application.contract.ContractData.membership;
import static br.com.erpcore.application.contract.ContractData.user;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.erpcore.application.identity.port.out.MembershipRepository;
import br.com.erpcore.domain.company.Company;
import br.com.erpcore.domain.identity.Membership;
import br.com.erpcore.domain.identity.Role;
import br.com.erpcore.domain.identity.User;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

/** O mesmo contrato roda contra o fake em memória e contra o adaptador de PostgreSQL. */
public abstract class MembershipRepositoryContract {

    private final User user = user(1);
    private final Company company = company(1);
    private final Company otherCompany = company(2);

    protected abstract MembershipRepository repository();

    /** Deixa de pé o usuário e as empresas que o vínculo referencia (chaves estrangeiras no banco real). */
    protected abstract void givenUserAndCompaniesExist(User user, Company... companies);

    protected abstract <T> T inTransaction(Supplier<T> work);

    private void save(Membership membership) {
        inTransaction(() -> {
            repository().save(membership);
            return null;
        });
    }

    @Test
    void givenASavedMembership_whenFindingByCompanyAndUser_thenEveryFieldComesBack() {
        givenUserAndCompaniesExist(user, company);
        Membership saved = membership(1, company, user);
        save(saved);

        Membership found = inTransaction(() -> repository().find(company.id(), user.id())).orElseThrow();

        assertThat(found.id()).isEqualTo(saved.id());
        assertThat(found.companyId()).isEqualTo(company.id());
        assertThat(found.userId()).isEqualTo(user.id());
        assertThat(found.role()).isEqualTo(Role.OWNER);
        assertThat(found.createdAt()).isEqualTo(saved.createdAt());
    }

    @Test
    void givenAMembershipInOneCompany_whenFindingInAnotherCompany_thenTheResultIsEmpty() {
        givenUserAndCompaniesExist(user, company, otherCompany);
        save(membership(1, company, user));

        assertThat(inTransaction(() -> repository().find(otherCompany.id(), user.id()))).isEmpty();
    }

    @Test
    void givenSavedMemberships_whenFindingByUser_thenAllOfThatUsersMembershipsComeBack() {
        givenUserAndCompaniesExist(user, company, otherCompany);
        save(membership(1, company, user));
        save(membership(2, otherCompany, user));

        assertThat(inTransaction(() -> repository().findByUserId(user.id())))
                .extracting(found -> found.companyId())
                .containsExactlyInAnyOrder(company.id(), otherCompany.id());
    }

    @Test
    void givenAUserWithoutMemberships_whenFindingByUser_thenTheListIsEmpty() {
        givenUserAndCompaniesExist(user, company);

        assertThat(inTransaction(() -> repository().findByUserId(user.id()))).isEmpty();
    }

    @Test
    void givenAMembershipSavedTwice_whenFindingByUser_thenItAppearsOnce() {
        givenUserAndCompaniesExist(user, company);
        Membership membership = membership(1, company, user);
        save(membership);
        save(membership);

        assertThat(inTransaction(() -> repository().findByUserId(user.id()))).hasSize(1);
    }
}
