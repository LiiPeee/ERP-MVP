package br.com.erpcore.application.identity;

import static br.com.erpcore.application.support.ApplicationFixture.CORRELATION_ID;
import static br.com.erpcore.application.support.ApplicationFixture.OWNER_EMAIL;
import static br.com.erpcore.application.support.ApplicationFixture.OWNER_PASSWORD;
import static br.com.erpcore.application.support.ApplicationFixture.validRegistration;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.erpcore.application.shared.port.out.AccessScope;
import br.com.erpcore.application.support.ApplicationFixture;
import br.com.erpcore.domain.audit.AuditEvent;
import br.com.erpcore.domain.audit.AuditEventType;
import br.com.erpcore.domain.audit.AuditOutcome;
import br.com.erpcore.domain.company.Company;
import br.com.erpcore.domain.company.CompanyId;
import br.com.erpcore.domain.identity.Email;
import br.com.erpcore.domain.identity.EmailAlreadyRegisteredException;
import br.com.erpcore.domain.identity.Membership;
import br.com.erpcore.domain.identity.Role;
import br.com.erpcore.domain.identity.User;
import br.com.erpcore.domain.identity.UserId;
import br.com.erpcore.domain.shared.DomainValidationException;
import org.junit.jupiter.api.Test;

class RegisterCompanyAndOwnerUseCaseTest {

    private final ApplicationFixture fixture = new ApplicationFixture();
    private final RegisterCompanyAndOwnerUseCase useCase = fixture.registerUseCase();

    @Test
    void givenValidData_whenRegistering_thenTheCompanyTheOwnerAndTheirMembershipAreCreated() {
        RegisterCompanyAndOwnerResult result = useCase.execute(validRegistration());

        Company company = fixture.companies.all().get(0);
        User owner = fixture.users.all().get(0);
        Membership membership = fixture.memberships.all().get(0);
        assertThat(result.companyId()).isEqualTo(company.id().value());
        assertThat(result.userId()).isEqualTo(owner.id().value());
        assertThat(company.name().value()).isEqualTo("Padaria Fictícia Ltda");
        assertThat(company.createdAt()).isEqualTo(ApplicationFixture.START);
        assertThat(owner.email()).isEqualTo(Email.of(OWNER_EMAIL));
        assertThat(owner.displayName().value()).isEqualTo("Ana Fictícia");
        assertThat(membership.companyId()).isEqualTo(company.id());
        assertThat(membership.userId()).isEqualTo(owner.id());
        assertThat(membership.role()).isEqualTo(Role.OWNER);
    }

    @Test
    void givenValidData_whenRegistering_thenThePasswordIsStoredOnlyAsAHash() {
        useCase.execute(validRegistration());

        String stored = fixture.users.all().get(0).passwordHash().value();
        assertThat(stored).doesNotContain(OWNER_PASSWORD);
        assertThat(fixture.passwordHasher.matches(OWNER_PASSWORD, stored)).isTrue();
    }

    @Test
    void givenValidData_whenRegistering_thenOneSuccessEventIsAuditedWithoutPersonalData() {
        RegisterCompanyAndOwnerResult result = useCase.execute(validRegistration());

        AuditEvent event = fixture.auditEvents.only();
        assertThat(event.type()).isEqualTo(AuditEventType.REGISTRATION);
        assertThat(event.outcome()).isEqualTo(AuditOutcome.SUCCESS);
        assertThat(event.companyId()).contains(new CompanyId(result.companyId()));
        assertThat(event.actorUserId()).contains(new UserId(result.userId()));
        assertThat(event.correlationId()).isEqualTo(CORRELATION_ID);
        assertThat(event.occurredAt()).isEqualTo(ApplicationFixture.START);
        assertThat(event.details().toString()).doesNotContain(OWNER_EMAIL).doesNotContain("Ana");
    }

    @Test
    void givenValidData_whenRegistering_thenEverythingHappensInOneCommittedTransactionScopedToTheNewCompany() {
        RegisterCompanyAndOwnerResult result = useCase.execute(validRegistration());

        assertThat(fixture.unitOfWork.commits()).isEqualTo(1);
        assertThat(fixture.unitOfWork.scopes()).containsExactly(
                new AccessScope.Registration(new CompanyId(result.companyId()), Email.of(OWNER_EMAIL)));
    }

    @Test
    void givenAnEmailAlreadyRegisteredInOtherCase_whenRegistering_thenItIsRejectedAndNothingNewIsCreated() {
        useCase.execute(validRegistration());
        RegisterCompanyAndOwnerCommand duplicate = new RegisterCompanyAndOwnerCommand("Outra Empresa Fictícia",
                "Beto Fictício", "ANA.FICTICIA@EXEMPLO.TEST", "outra-senha-sintetica-1", "correlacao-sintetica-2");

        assertThatThrownBy(() -> useCase.execute(duplicate)).isInstanceOf(EmailAlreadyRegisteredException.class);

        assertThat(fixture.companies.all()).hasSize(1);
        assertThat(fixture.users.all()).hasSize(1);
        assertThat(fixture.memberships.all()).hasSize(1);
    }

    @Test
    void givenADuplicateEmail_whenRegistering_thenTheFailureIsAuditedAndCommittedBeforeTheErrorIsRaised() {
        useCase.execute(validRegistration());
        RegisterCompanyAndOwnerCommand duplicate = new RegisterCompanyAndOwnerCommand("Outra Empresa Fictícia",
                "Beto Fictício", OWNER_EMAIL, "outra-senha-sintetica-1", "correlacao-sintetica-2");

        assertThatThrownBy(() -> useCase.execute(duplicate)).isInstanceOf(EmailAlreadyRegisteredException.class);

        AuditEvent failure = fixture.auditEvents.all().get(1);
        assertThat(failure.type()).isEqualTo(AuditEventType.REGISTRATION);
        assertThat(failure.outcome()).isEqualTo(AuditOutcome.FAILURE);
        assertThat(failure.details()).containsEntry("reason", "EMAIL_ALREADY_REGISTERED");
        assertThat(failure.correlationId()).isEqualTo("correlacao-sintetica-2");
        assertThat(failure.details().toString()).doesNotContain(OWNER_EMAIL);
        assertThat(fixture.unitOfWork.commits()).isEqualTo(2);
        assertThat(fixture.unitOfWork.rollbacks()).isZero();
    }

    @Test
    void givenADuplicateEmail_whenRegistering_thenThePasswordIsStillHashedSoTimingDoesNotRevealTheAccount() {
        useCase.execute(validRegistration());
        int hashesAfterFirst = fixture.passwordHasher.hashCalls();
        RegisterCompanyAndOwnerCommand duplicate = new RegisterCompanyAndOwnerCommand("Outra Empresa Fictícia",
                "Beto Fictício", OWNER_EMAIL, "outra-senha-sintetica-1", "correlacao-sintetica-2");

        assertThatThrownBy(() -> useCase.execute(duplicate)).isInstanceOf(EmailAlreadyRegisteredException.class);

        assertThat(fixture.passwordHasher.hashCalls()).isEqualTo(hashesAfterFirst + 1);
    }

    @Test
    void givenInvalidInput_whenRegistering_thenItIsRejectedBeforeAnyTransactionStarts() {
        RegisterCompanyAndOwnerCommand shortPassword = new RegisterCompanyAndOwnerCommand("Padaria Fictícia Ltda",
                "Ana Fictícia", OWNER_EMAIL, "curta", CORRELATION_ID);
        RegisterCompanyAndOwnerCommand badEmail = new RegisterCompanyAndOwnerCommand("Padaria Fictícia Ltda",
                "Ana Fictícia", "isso-nao-e-email", OWNER_PASSWORD, CORRELATION_ID);
        RegisterCompanyAndOwnerCommand noCompanyName = new RegisterCompanyAndOwnerCommand(" ", "Ana Fictícia",
                OWNER_EMAIL, OWNER_PASSWORD, CORRELATION_ID);
        RegisterCompanyAndOwnerCommand noOwnerName = new RegisterCompanyAndOwnerCommand("Padaria Fictícia Ltda", null,
                OWNER_EMAIL, OWNER_PASSWORD, CORRELATION_ID);

        assertThatThrownBy(() -> useCase.execute(shortPassword)).isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> useCase.execute(badEmail)).isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> useCase.execute(noCompanyName)).isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> useCase.execute(noOwnerName)).isInstanceOf(DomainValidationException.class);

        assertThat(fixture.unitOfWork.scopes()).isEmpty();
        assertThat(fixture.auditEvents.all()).isEmpty();
    }

    @Test
    void givenACommand_whenPrintingIt_thenNeitherThePasswordNorTheEmailAppears() {
        String printed = validRegistration().toString();

        assertThat(printed).doesNotContain(OWNER_PASSWORD).doesNotContain(OWNER_EMAIL);
    }
}
