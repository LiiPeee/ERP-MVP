package br.com.erpcore.application.contract;

import static br.com.erpcore.application.contract.BusinessContractData.contact;
import static br.com.erpcore.application.contract.BusinessContractData.vehicle;
import static br.com.erpcore.application.contract.ContractData.company;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.erpcore.application.vehicle.port.out.VehicleRepository;
import br.com.erpcore.domain.company.Company;
import br.com.erpcore.domain.contact.Contact;
import br.com.erpcore.domain.vehicle.LicensePlate;
import br.com.erpcore.domain.vehicle.PlateAlreadyRegisteredException;
import br.com.erpcore.domain.vehicle.Vehicle;
import br.com.erpcore.domain.vehicle.VehicleId;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** O mesmo contrato roda contra o fake em memória e contra o adaptador de PostgreSQL. */
public abstract class VehicleRepositoryContract extends BusinessContract {

    private final Company company = company(1);
    private final Company otherCompany = company(2);
    private final Contact owner = contact(1, company);
    private final Contact otherOwner = contact(2, otherCompany);

    protected abstract VehicleRepository repository();

    private void save(Vehicle vehicle) {
        inTransaction(() -> repository().save(vehicle));
    }

    private void prepare() {
        givenCompaniesExist(company, otherCompany);
        givenContactsExist(owner, otherOwner);
    }

    @Test
    void givenASavedVehicle_whenFindingItById_thenEveryFieldComesBack() {
        prepare();
        Vehicle saved = vehicle(1, company, owner);
        save(saved);

        Vehicle found = inTransaction(() -> repository().findById(company.id(), saved.id())).orElseThrow();

        assertThat(found.id()).isEqualTo(saved.id());
        assertThat(found.companyId()).isEqualTo(company.id());
        assertThat(found.contactId()).isEqualTo(owner.id());
        assertThat(found.plate()).isEqualTo(saved.plate());
        assertThat(found.brand()).isEqualTo("Fiat");
        assertThat(found.model()).isEqualTo(saved.model());
        assertThat(found.year()).isEqualTo(2012);
        assertThat(found.odometerKm()).hasValue(123_456);
        assertThat(found.createdAt()).isEqualTo(saved.createdAt());
    }

    @Test
    void givenAVehicleWithoutOdometer_whenFindingIt_thenTheOdometerIsAbsent() {
        prepare();
        Vehicle saved = Vehicle.register(new VehicleId(UUID.randomUUID()), company.id(), owner.id(), LicensePlate.of("XYZ9A87"),
                "VW", "Gol", 2020, null, ContractData.NOW);
        save(saved);

        assertThat(inTransaction(() -> repository().findById(company.id(), saved.id())).orElseThrow().odometerKm()).isEmpty();
    }

    @Test
    void givenAVehicleOfAnotherCompany_whenFindingItById_thenItIsNotFound() {
        prepare();
        Vehicle saved = vehicle(1, company, owner);
        save(saved);

        assertThat(inTransaction(() -> repository().findById(otherCompany.id(), saved.id()))).isEmpty();
        assertThat(inTransaction(() -> repository().findById(company.id(), new VehicleId(UUID.randomUUID())))).isEmpty();
    }

    @Test
    void givenASavedVehicle_whenFindingByItsPlate_thenItIsFoundOnlyInItsOwnCompany() {
        prepare();
        Vehicle saved = vehicle(1, company, owner);
        save(saved);

        assertThat(inTransaction(() -> repository().findByPlate(company.id(), saved.plate()))).hasValueSatisfying(
                found -> assertThat(found.id()).isEqualTo(saved.id()));
        assertThat(inTransaction(() -> repository().findByPlate(otherCompany.id(), saved.plate()))).isEmpty();
        assertThat(inTransaction(() -> repository().findByPlate(company.id(), LicensePlate.of("ZZZ9999")))).isEmpty();
    }

    @Test
    void givenTheSamePlateInTwoCompanies_whenSavingBoth_thenBothAreAccepted() {
        prepare();
        Vehicle first = vehicle(1, company, owner);
        Vehicle second = Vehicle.register(new VehicleId(UUID.randomUUID()), otherCompany.id(), otherOwner.id(), first.plate(),
                "Fiat", "Uno", 2012, null, ContractData.NOW);

        save(first);
        save(second);

        assertThat(inTransaction(() -> repository().findByPlate(otherCompany.id(), first.plate()))).isPresent();
    }

    @Test
    void givenAnotherVehicleWithTheSamePlateInTheSameCompany_whenSaving_thenItIsRefused() {
        prepare();
        Vehicle first = vehicle(1, company, owner);
        Vehicle duplicate = Vehicle.register(new VehicleId(UUID.randomUUID()), company.id(), owner.id(), first.plate(), "VW", "Gol",
                2020, null, ContractData.NOW);
        save(first);

        assertThatThrownBy(() -> save(duplicate)).isInstanceOf(PlateAlreadyRegisteredException.class);
    }

    @Test
    void givenASavedVehicle_whenSavingItAgainWithNewData_thenItIsUpdatedAndNotSeenAsADuplicate() {
        prepare();
        Vehicle original = vehicle(1, company, owner);
        save(original);
        Vehicle updated = Vehicle.register(original.id(), company.id(), owner.id(), original.plate(), "Fiat", "Uno Mille", 2012, 130_000,
                original.createdAt());

        save(updated);

        assertThat(inTransaction(() -> repository().findById(company.id(), original.id())).orElseThrow().odometerKm()).hasValue(130_000);
    }

    @Test
    void givenVehiclesOfTwoContacts_whenListingByContact_thenOnlyThatContactsVehiclesComeBackOldestFirst() {
        prepare();
        Contact second = contact(3, company);
        givenContactsExist(second);
        save(vehicle(12, company, owner));
        save(vehicle(11, company, owner));
        save(vehicle(13, company, second));

        assertThat(inTransaction(() -> repository().findByContact(company.id(), owner.id())))
                .extracting(found -> found.id().value()).containsExactly(ContractData.uuid(11), ContractData.uuid(12));
        assertThat(inTransaction(() -> repository().findByContact(otherCompany.id(), owner.id()))).isEmpty();
    }
}
