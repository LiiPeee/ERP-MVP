package br.com.erpcore.application.vehicle;

import static br.com.erpcore.application.support.ApplicationFixture.CORRELATION_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.erpcore.application.contact.ContactView;
import br.com.erpcore.application.contact.CreateContactCommand;
import br.com.erpcore.application.identity.AuthenticatedPrincipal;
import br.com.erpcore.application.identity.RegisterCompanyAndOwnerResult;
import br.com.erpcore.application.support.ApplicationFixture;
import br.com.erpcore.domain.audit.AuditEvent;
import br.com.erpcore.domain.audit.AuditEventType;
import br.com.erpcore.domain.contact.ContactNotFoundException;
import br.com.erpcore.domain.shared.DomainValidationException;
import br.com.erpcore.domain.vehicle.PlateAlreadyRegisteredException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VehicleUseCasesTest {

    private final ApplicationFixture fixture = new ApplicationFixture();
    private final RegisterCompanyAndOwnerResult owner = fixture.registerOwner();
    private final AuthenticatedPrincipal principal = fixture.principalOf(owner);
    private final ContactView client = contact(principal, "Cliente Fictício", "11987654321");

    private ContactView contact(AuthenticatedPrincipal who, String name, String phone) {
        return fixture.createContactUseCase().execute(new CreateContactCommand(who, name, phone, null, CORRELATION_ID));
    }

    private VehicleView register(AuthenticatedPrincipal who, UUID contactId, String plate) {
        return fixture.registerVehicleUseCase().execute(new RegisterVehicleCommand(who, contactId, plate, "Fiat", "Uno", 2012, 123_456, CORRELATION_ID));
    }

    @Test
    void givenValidData_whenRegisteringAVehicle_thenTheViewHasAMaskedPlateAndTheStoredPlateIsNormalized() {
        VehicleView view = register(principal, client.id(), "abc-1234");

        assertThat(view.contactId()).isEqualTo(client.id());
        assertThat(view.plateMasked()).isEqualTo("****234");
        assertThat(view.brand()).isEqualTo("Fiat");
        assertThat(view.model()).isEqualTo("Uno");
        assertThat(view.year()).isEqualTo(2012);
        assertThat(view.odometerKm()).isEqualTo(123_456);
        assertThat(fixture.vehicles.all().get(0).plate().value()).isEqualTo("ABC1234");
    }

    @Test
    void givenNoOdometer_whenRegistering_thenItIsAbsent() {
        VehicleView view = fixture.registerVehicleUseCase().execute(new RegisterVehicleCommand(principal, client.id(), "ABC1234",
                "Fiat", "Uno", 2012, null, CORRELATION_ID));

        assertThat(view.odometerKm()).isNull();
    }

    @Test
    void givenAVehicleRegistered_whenReadingTheAudit_thenThePlateNeverAppears() {
        VehicleView view = register(principal, client.id(), "ABC1234");

        AuditEvent event = fixture.auditEvents.last();
        assertThat(event.type()).isEqualTo(AuditEventType.VEHICLE_REGISTERED);
        assertThat(event.details()).containsEntry("vehicleId", view.id().toString());
        assertThat(event.details().toString()).doesNotContain("ABC");
    }

    @Test
    void givenAContactThatDoesNotExistOrBelongsToAnotherCompany_whenRegistering_thenItIsNotFound() {
        ContactView foreign = contact(fixture.principalOf(fixture.registerOtherOwner()), "Cliente de outra oficina", "11987654399");

        assertThatThrownBy(() -> register(principal, UUID.randomUUID(), "ABC1234")).isInstanceOf(ContactNotFoundException.class);
        assertThatThrownBy(() -> register(principal, foreign.id(), "ABC1234")).isInstanceOf(ContactNotFoundException.class);
        assertThat(fixture.vehicles.all()).isEmpty();
    }

    @Test
    void givenAPlateAlreadyInTheCompany_whenRegisteringAgain_thenItIsRefused() {
        register(principal, client.id(), "ABC1234");
        ContactView second = contact(principal, "Outro Cliente", "11987654322");

        assertThatThrownBy(() -> register(principal, second.id(), "abc 1234")).isInstanceOf(PlateAlreadyRegisteredException.class);

        assertThat(fixture.vehicles.all()).hasSize(1);
    }

    @Test
    void givenTheSamePlateInAnotherCompany_whenRegistering_thenItIsAccepted() {
        AuthenticatedPrincipal other = fixture.principalOf(fixture.registerOtherOwner());
        ContactView otherClient = contact(other, "Cliente de outra oficina", "11987654399");
        register(principal, client.id(), "ABC1234");

        register(other, otherClient.id(), "ABC1234");

        assertThat(fixture.vehicles.all()).hasSize(2);
    }

    @Test
    void givenInvalidData_whenRegistering_thenItIsRejectedWithTheFieldName() {
        assertThatThrownBy(() -> register(principal, client.id(), "123"))
                .isInstanceOfSatisfying(DomainValidationException.class, e -> assertThat(e.field()).isEqualTo("licensePlate"));
        assertThatThrownBy(() -> fixture.registerVehicleUseCase().execute(new RegisterVehicleCommand(principal, client.id(), "ABC1234",
                "Fiat", "Uno", 1800, null, CORRELATION_ID)))
                .isInstanceOfSatisfying(DomainValidationException.class, e -> assertThat(e.field()).isEqualTo("year"));
        assertThat(fixture.vehicles.all()).isEmpty();
    }

    @Test
    void givenAContactWithVehicles_whenListingThem_thenTheyComeBackOldestFirstAndOnlyThatContactsVehicles() {
        ContactView second = contact(principal, "Outro Cliente", "11987654322");
        register(principal, client.id(), "ABC1234");
        fixture.clock.advance(java.time.Duration.ofMinutes(1));
        register(principal, client.id(), "XYZ9A87");
        register(principal, second.id(), "QWE1R23");

        List<VehicleView> listed = fixture.listVehiclesUseCase().execute(new ListVehiclesQuery(principal, client.id()));

        assertThat(listed).extracting(VehicleView::plateMasked).containsExactly("****234", "****A87");
    }

    @Test
    void givenAContactOfAnotherCompany_whenListingItsVehicles_thenItIsNotFound() {
        ContactView foreign = contact(fixture.principalOf(fixture.registerOtherOwner()), "Cliente de outra oficina", "11987654399");

        assertThatThrownBy(() -> fixture.listVehiclesUseCase().execute(new ListVehiclesQuery(principal, foreign.id())))
                .isInstanceOf(ContactNotFoundException.class);
    }

    @Test
    void givenSomeoneWhoIsNotAMember_whenRegisteringOrListing_thenAccessIsDenied() {
        AuthenticatedPrincipal outsider = new AuthenticatedPrincipal(UUID.randomUUID(), owner.companyId());

        assertThatThrownBy(() -> register(outsider, client.id(), "ABC1234")).isInstanceOf(br.com.erpcore.domain.identity.AccessDeniedException.class);
        assertThatThrownBy(() -> fixture.listVehiclesUseCase().execute(new ListVehiclesQuery(outsider, client.id())))
                .isInstanceOf(br.com.erpcore.domain.identity.AccessDeniedException.class);
    }
}
