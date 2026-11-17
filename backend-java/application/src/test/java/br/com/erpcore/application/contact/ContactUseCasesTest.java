package br.com.erpcore.application.contact;

import static br.com.erpcore.application.support.ApplicationFixture.CORRELATION_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.erpcore.application.identity.AuthenticatedPrincipal;
import br.com.erpcore.application.identity.RegisterCompanyAndOwnerResult;
import br.com.erpcore.application.support.ApplicationFixture;
import br.com.erpcore.domain.audit.AuditEvent;
import br.com.erpcore.domain.audit.AuditEventType;
import br.com.erpcore.domain.company.CompanyId;
import br.com.erpcore.domain.contact.Contact;
import br.com.erpcore.domain.contact.PhoneAlreadyRegisteredException;
import br.com.erpcore.domain.contact.PhoneNumber;
import br.com.erpcore.domain.shared.DomainValidationException;
import java.util.List;
import org.junit.jupiter.api.Test;

class ContactUseCasesTest {

    private final ApplicationFixture fixture = new ApplicationFixture();
    private final RegisterCompanyAndOwnerResult owner = fixture.registerOwner();
    private final AuthenticatedPrincipal principal = fixture.principalOf(owner);

    private ContactView create(AuthenticatedPrincipal who, String name, String phone, String email) {
        return fixture.createContactUseCase().execute(new CreateContactCommand(who, name, phone, email, CORRELATION_ID));
    }

    private List<ContactView> search(AuthenticatedPrincipal who, String fragment, int limit) {
        return fixture.listContactsUseCase().execute(new ListContactsQuery(who, fragment, limit));
    }

    @Test
    void givenValidData_whenCreatingAContact_thenTheViewIsMaskedAndThePhoneIsStoredNormalized() {
        ContactView view = create(principal, "  Cliente Fictício ", "(11) 98765-4321", "cliente@exemplo.test");

        assertThat(view.name()).isEqualTo("Cliente Fictício");
        assertThat(view.phoneMasked()).isEqualTo("(11) *****-4321");
        assertThat(view.emailMasked()).isEqualTo("c***@exemplo.test");
        Contact stored = fixture.contacts.all().get(0);
        assertThat(stored.phone()).isEqualTo(PhoneNumber.of("+5511987654321"));
        assertThat(stored.companyId()).isEqualTo(new CompanyId(owner.companyId()));
    }

    @Test
    void givenNoEmail_whenCreatingAContact_thenTheMaskedEmailIsAbsent() {
        assertThat(create(principal, "Cliente Fictício", "11987654321", null).emailMasked()).isNull();
    }

    @Test
    void givenAContactCreated_whenReadingTheAudit_thenItIsRecordedWithoutPersonalData() {
        ContactView view = create(principal, "Cliente Fictício", "11987654321", "cliente@exemplo.test");

        AuditEvent event = fixture.auditEvents.last();
        assertThat(event.type()).isEqualTo(AuditEventType.CONTACT_CREATED);
        assertThat(event.details()).containsEntry("contactId", view.id().toString());
        assertThat(event.details().toString()).doesNotContain("Cliente", "987654321", "cliente@");
    }

    @Test
    void givenAPhoneAlreadyInTheCompany_whenCreatingAnotherContact_thenItIsRefusedAndNothingIsAudited() {
        create(principal, "Primeiro", "11987654321", null);
        int events = fixture.auditEvents.all().size();

        assertThatThrownBy(() -> create(principal, "Segundo", "+55 (11) 98765-4321", null)).isInstanceOf(PhoneAlreadyRegisteredException.class);

        assertThat(fixture.contacts.all()).hasSize(1);
        assertThat(fixture.auditEvents.all()).hasSize(events);
    }

    @Test
    void givenTheSamePhoneInAnotherCompany_whenCreatingAContact_thenItIsAccepted() {
        AuthenticatedPrincipal other = fixture.principalOf(fixture.registerOtherOwner());
        create(principal, "Primeiro", "11987654321", null);

        create(other, "Mesmo telefone, outra oficina", "11987654321", null);

        assertThat(fixture.contacts.all()).hasSize(2);
    }

    @Test
    void givenInvalidData_whenCreatingAContact_thenItIsRejectedWithTheFieldName() {
        assertThatThrownBy(() -> create(principal, " ", "11987654321", null))
                .isInstanceOfSatisfying(DomainValidationException.class, e -> assertThat(e.field()).isEqualTo("contactName"));
        assertThatThrownBy(() -> create(principal, "Cliente", "123", null))
                .isInstanceOfSatisfying(DomainValidationException.class, e -> assertThat(e.field()).isEqualTo("phone"));
        assertThatThrownBy(() -> create(principal, "Cliente", "11987654321", "isto-nao-e-email"))
                .isInstanceOfSatisfying(DomainValidationException.class, e -> assertThat(e.field()).isEqualTo("email"));
        assertThat(fixture.contacts.all()).isEmpty();
    }

    @Test
    void givenSeveralContacts_whenSearchingByPartOfTheName_thenOnlyMatchesOfTheOwnCompanyComeBack() {
        AuthenticatedPrincipal other = fixture.principalOf(fixture.registerOtherOwner());
        create(principal, "José da Silva Fictício", "11987654321", null);
        create(principal, "Maria Fictícia", "11987654322", null);
        create(other, "José de Outra Oficina", "11987654323", null);

        List<ContactView> found = search(principal, "jose", 10);

        assertThat(found).extracting(ContactView::name).containsExactly("José da Silva Fictício");
    }

    @Test
    void givenALimitAboveTheCeiling_whenSearching_thenTheCeilingApplies() {
        for (int i = 0; i < ListContactsUseCase.MAX_LIMIT + 5; i++) {
            create(principal, "Cliente Fictício " + i, "119" + String.format("%08d", i), null);
        }

        assertThat(search(principal, "", 1_000)).hasSize(ListContactsUseCase.MAX_LIMIT);
        assertThat(search(principal, "", 0)).hasSize(ListContactsUseCase.DEFAULT_LIMIT);
        assertThat(search(principal, null, -3)).hasSize(ListContactsUseCase.DEFAULT_LIMIT);
    }

    @Test
    void givenSomeoneWhoIsNotAMember_whenCreatingOrSearching_thenAccessIsDenied() {
        AuthenticatedPrincipal outsider = new AuthenticatedPrincipal(java.util.UUID.randomUUID(), owner.companyId());

        assertThatThrownBy(() -> create(outsider, "Cliente", "11987654321", null))
                .isInstanceOf(br.com.erpcore.domain.identity.AccessDeniedException.class);
        assertThatThrownBy(() -> search(outsider, "", 10)).isInstanceOf(br.com.erpcore.domain.identity.AccessDeniedException.class);
    }
}
