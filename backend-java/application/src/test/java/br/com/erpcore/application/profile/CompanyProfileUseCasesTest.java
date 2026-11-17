package br.com.erpcore.application.profile;

import static br.com.erpcore.application.support.ApplicationFixture.CORRELATION_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.erpcore.application.identity.AuthenticatedPrincipal;
import br.com.erpcore.application.identity.RegisterCompanyAndOwnerResult;
import br.com.erpcore.application.shared.port.out.AccessScope;
import br.com.erpcore.application.support.ApplicationFixture;
import br.com.erpcore.domain.audit.AuditEvent;
import br.com.erpcore.domain.audit.AuditEventType;
import br.com.erpcore.domain.audit.AuditOutcome;
import br.com.erpcore.domain.company.CompanyId;
import br.com.erpcore.domain.identity.AccessDeniedException;
import br.com.erpcore.domain.identity.UserId;
import br.com.erpcore.domain.shared.DomainValidationException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CompanyProfileUseCasesTest {

    private final ApplicationFixture fixture = new ApplicationFixture();
    private final RegisterCompanyAndOwnerResult owner = fixture.registerOwner();
    private final AuthenticatedPrincipal principal = fixture.principalOf(owner);

    private UpdateCompanyProfileCommand update(AuthenticatedPrincipal who, String displayName, int validityDays) {
        return new UpdateCompanyProfileCommand(who, displayName, "11987654321", "Rua Fictícia, 100", "Seg a sex, 8h às 18h",
                validityDays, "Obrigado pela preferência!", CORRELATION_ID);
    }

    @Test
    void givenACompanyThatNeverFilledTheProfile_whenReadingIt_thenTheDefaultsComeBack() {
        CompanyProfileView view = fixture.getCompanyProfileUseCase().execute(new GetCompanyProfileQuery(principal));

        assertThat(view.displayName()).isEqualTo("Padaria Fictícia Ltda");
        assertThat(view.defaultValidityDays()).isEqualTo(7);
        assertThat(view.businessPhoneMasked()).isNull();
        assertThat(view.address()).isEmpty();
        assertThat(view.signatureMessage()).isEmpty();
    }

    @Test
    void givenTheOwner_whenUpdatingTheProfile_thenReadingItBackShowsTheNewDataWithAMaskedPhone() {
        fixture.updateCompanyProfileUseCase().execute(update(principal, "Oficina do Zé Fictício", 10));

        CompanyProfileView view = fixture.getCompanyProfileUseCase().execute(new GetCompanyProfileQuery(principal));

        assertThat(view.displayName()).isEqualTo("Oficina do Zé Fictício");
        assertThat(view.businessPhoneMasked()).isEqualTo("(11) *****-4321");
        assertThat(view.address()).isEqualTo("Rua Fictícia, 100");
        assertThat(view.openingHours()).isEqualTo("Seg a sex, 8h às 18h");
        assertThat(view.defaultValidityDays()).isEqualTo(10);
        assertThat(view.signatureMessage()).isEqualTo("Obrigado pela preferência!");
    }

    @Test
    void givenAnUpdate_whenItSucceeds_thenItRunsInATenantScopeAndIsAuditedWithoutPersonalData() {
        fixture.updateCompanyProfileUseCase().execute(update(principal, "Oficina do Zé Fictício", 10));

        assertThat(fixture.unitOfWork.scopes().get(fixture.unitOfWork.scopes().size() - 1))
                .isEqualTo(new AccessScope.Tenant(new UserId(owner.userId()), new CompanyId(owner.companyId())));
        AuditEvent event = fixture.auditEvents.last();
        assertThat(event.type()).isEqualTo(AuditEventType.COMPANY_PROFILE_UPDATED);
        assertThat(event.outcome()).isEqualTo(AuditOutcome.SUCCESS);
        assertThat(event.actorUserId()).contains(new UserId(owner.userId()));
        assertThat(event.companyId()).contains(new CompanyId(owner.companyId()));
        assertThat(event.correlationId()).isEqualTo(CORRELATION_ID);
        assertThat(event.details().toString()).doesNotContain("Zé", "987654321", "Rua Fictícia");
    }

    @Test
    void givenInvalidData_whenUpdating_thenItIsRejectedAndNothingIsSavedOrAudited() {
        int eventsBefore = fixture.auditEvents.all().size();

        assertThatThrownBy(() -> fixture.updateCompanyProfileUseCase().execute(update(principal, "Oficina", 0)))
                .isInstanceOfSatisfying(DomainValidationException.class, e -> assertThat(e.field()).isEqualTo("defaultValidityDays"));
        assertThatThrownBy(() -> fixture.updateCompanyProfileUseCase().execute(new UpdateCompanyProfileCommand(principal, "Oficina",
                "telefone ruim", null, null, 7, null, CORRELATION_ID)))
                .isInstanceOfSatisfying(DomainValidationException.class, e -> assertThat(e.field()).isEqualTo("phone"));

        assertThat(fixture.auditEvents.all()).hasSize(eventsBefore);
        assertThat(fixture.getCompanyProfileUseCase().execute(new GetCompanyProfileQuery(principal)).defaultValidityDays()).isEqualTo(7);
    }

    @Test
    void givenNoBusinessPhone_whenUpdating_thenThePhoneIsAbsent() {
        fixture.updateCompanyProfileUseCase().execute(new UpdateCompanyProfileCommand(principal, "Oficina", null, null, null, 7, null, CORRELATION_ID));

        assertThat(fixture.getCompanyProfileUseCase().execute(new GetCompanyProfileQuery(principal)).businessPhoneMasked()).isNull();
    }

    @Test
    void givenABlankBusinessPhone_whenUpdating_thenItIsTreatedAsNoPhone() {
        fixture.updateCompanyProfileUseCase().execute(new UpdateCompanyProfileCommand(principal, "Oficina", "   ", null, null, 7, null, CORRELATION_ID));

        assertThat(fixture.getCompanyProfileUseCase().execute(new GetCompanyProfileQuery(principal)).businessPhoneMasked()).isNull();
    }

    @Test
    void givenTwoCompanies_whenOneUpdatesItsProfile_thenTheOtherIsUntouched() {
        RegisterCompanyAndOwnerResult other = fixture.registerOtherOwner();
        AuthenticatedPrincipal otherPrincipal = fixture.principalOf(other);

        fixture.updateCompanyProfileUseCase().execute(update(principal, "Só da primeira", 30));

        CompanyProfileView otherView = fixture.getCompanyProfileUseCase().execute(new GetCompanyProfileQuery(otherPrincipal));
        assertThat(otherView.displayName()).isEqualTo("Oficina Fictícia ME");
        assertThat(otherView.defaultValidityDays()).isEqualTo(7);
    }

    @Test
    void givenSomeoneWhoIsNotAMemberOfTheCompany_whenReadingOrUpdating_thenAccessIsDenied() {
        AuthenticatedPrincipal outsider = new AuthenticatedPrincipal(UUID.randomUUID(), owner.companyId());

        assertThatThrownBy(() -> fixture.getCompanyProfileUseCase().execute(new GetCompanyProfileQuery(outsider)))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> fixture.updateCompanyProfileUseCase().execute(update(outsider, "Oficina", 7)))
                .isInstanceOf(AccessDeniedException.class);
    }
}
