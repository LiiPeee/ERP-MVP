package br.com.erpcore.application.contract;

import static br.com.erpcore.application.contract.BusinessContractData.contact;
import static br.com.erpcore.application.contract.BusinessContractData.contactWithEmail;
import static br.com.erpcore.application.contract.BusinessContractData.namedContact;
import static br.com.erpcore.application.contract.ContractData.company;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.erpcore.application.contact.port.out.ContactRepository;
import br.com.erpcore.domain.company.Company;
import br.com.erpcore.domain.contact.Contact;
import br.com.erpcore.domain.contact.ContactId;
import br.com.erpcore.domain.contact.ContactName;
import br.com.erpcore.domain.contact.PhoneAlreadyRegisteredException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** O mesmo contrato roda contra o fake em memória e contra o adaptador de PostgreSQL. */
public abstract class ContactRepositoryContract extends BusinessContract {

    private final Company company = company(1);
    private final Company otherCompany = company(2);

    protected abstract ContactRepository repository();

    private void save(Contact contact) {
        inTransaction(() -> repository().save(contact));
    }

    @Test
    void givenASavedContactWithEmail_whenFindingItById_thenEveryFieldComesBack() {
        givenCompaniesExist(company);
        Contact saved = contactWithEmail(1, company);
        save(saved);

        Contact found = inTransaction(() -> repository().findById(company.id(), saved.id())).orElseThrow();

        assertThat(found.id()).isEqualTo(saved.id());
        assertThat(found.companyId()).isEqualTo(company.id());
        assertThat(found.name().value()).isEqualTo(saved.name().value());
        assertThat(found.phone()).isEqualTo(saved.phone());
        assertThat(found.email()).isEqualTo(saved.email());
        assertThat(found.createdAt()).isEqualTo(saved.createdAt());
    }

    @Test
    void givenASavedContactWithoutEmail_whenFindingItById_thenTheEmailIsAbsent() {
        givenCompaniesExist(company);
        Contact saved = contact(1, company);
        save(saved);

        assertThat(inTransaction(() -> repository().findById(company.id(), saved.id())).orElseThrow().email()).isEmpty();
    }

    @Test
    void givenNoSuchContact_whenFindingItById_thenTheResultIsEmpty() {
        givenCompaniesExist(company);

        assertThat(inTransaction(() -> repository().findById(company.id(), new ContactId(UUID.randomUUID())))).isEmpty();
    }

    @Test
    void givenAContactOfAnotherCompany_whenFindingItById_thenItIsNotFound() {
        givenCompaniesExist(company, otherCompany);
        Contact saved = contact(1, company);
        save(saved);

        assertThat(inTransaction(() -> repository().findById(otherCompany.id(), saved.id()))).isEmpty();
    }

    @Test
    void givenASavedContact_whenFindingByItsPhone_thenItIsFoundOnlyInItsOwnCompany() {
        givenCompaniesExist(company, otherCompany);
        Contact saved = contact(1, company);
        save(saved);

        assertThat(inTransaction(() -> repository().findByPhone(company.id(), saved.phone()))).hasValueSatisfying(
                found -> assertThat(found.id()).isEqualTo(saved.id()));
        assertThat(inTransaction(() -> repository().findByPhone(otherCompany.id(), saved.phone()))).isEmpty();
    }

    @Test
    void givenTheSamePhoneInTwoCompanies_whenSavingBoth_thenBothAreAccepted() {
        givenCompaniesExist(company, otherCompany);
        Contact first = contact(1, company);
        Contact second = Contact.create(new ContactId(UUID.randomUUID()), otherCompany.id(), ContactName.of("Outro Cliente Fictício"),
                first.phone(), null, first.createdAt());

        save(first);
        save(second);

        assertThat(inTransaction(() -> repository().findByPhone(otherCompany.id(), first.phone()))).isPresent();
    }

    @Test
    void givenAnotherContactWithTheSamePhoneInTheSameCompany_whenSaving_thenItIsRefused() {
        givenCompaniesExist(company);
        Contact first = contact(1, company);
        Contact duplicate = Contact.create(new ContactId(UUID.randomUUID()), company.id(), ContactName.of("Outro Cliente Fictício"),
                first.phone(), null, first.createdAt());
        save(first);

        assertThatThrownBy(() -> save(duplicate)).isInstanceOf(PhoneAlreadyRegisteredException.class);
        assertThat(inTransaction(() -> repository().findByPhone(company.id(), first.phone())).orElseThrow().id()).isEqualTo(first.id());
    }

    @Test
    void givenASavedContact_whenSavingItAgainWithANewName_thenItIsUpdatedAndNotSeenAsADuplicate() {
        givenCompaniesExist(company);
        Contact original = contact(1, company);
        save(original);
        Contact renamed = Contact.create(original.id(), company.id(), ContactName.of("Nome Novo Fictício"), original.phone(), null,
                original.createdAt());

        save(renamed);

        assertThat(inTransaction(() -> repository().findById(company.id(), original.id())).orElseThrow().name().value())
                .isEqualTo("Nome Novo Fictício");
    }

    @Test
    void givenSeveralContacts_whenSearchingByPartOfTheName_thenCaseAndAccentsAreIgnoredAndNewestComesFirst() {
        givenCompaniesExist(company, otherCompany);
        save(namedContact(1, company, "José da Silva Fictício"));
        save(namedContact(2, company, "Maria Fictícia"));
        save(namedContact(3, company, "Josefa Fictícia"));
        save(namedContact(4, otherCompany, "José de Outra Oficina"));

        List<Contact> found = inTransaction(() -> repository().searchByName(company.id(), "JOSE", 10));

        assertThat(found).extracting(contact -> contact.name().value()).containsExactly("Josefa Fictícia", "José da Silva Fictício");
    }

    @Test
    void givenAFragmentThatMatchesNothing_whenSearching_thenTheListIsEmpty() {
        givenCompaniesExist(company);
        save(contact(1, company));

        assertThat(inTransaction(() -> repository().searchByName(company.id(), "zzz", 10))).isEmpty();
    }

    @Test
    void givenABlankFragment_whenSearching_thenTheNewestContactsComeBackUpToTheLimit() {
        givenCompaniesExist(company);
        save(contact(1, company));
        save(contact(2, company));
        save(contact(3, company));

        List<Contact> found = inTransaction(() -> repository().searchByName(company.id(), "  ", 2));

        assertThat(found).extracting(contact -> contact.id().value()).containsExactly(
                ContractData.uuid(3), ContractData.uuid(2));
    }

    @Test
    void givenAFragmentWithWildcardCharacters_whenSearching_thenTheyAreTakenLiterally() {
        givenCompaniesExist(company);
        save(namedContact(1, company, "Cliente 100% Fictício"));
        save(namedContact(2, company, "Cliente Qualquer"));

        assertThat(inTransaction(() -> repository().searchByName(company.id(), "%", 10))).hasSize(1);
        assertThat(inTransaction(() -> repository().searchByName(company.id(), "_", 10))).isEmpty();
    }
}
