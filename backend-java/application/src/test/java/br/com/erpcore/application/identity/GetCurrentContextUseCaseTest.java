package br.com.erpcore.application.identity;

import static br.com.erpcore.application.support.ApplicationFixture.CORRELATION_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.erpcore.application.shared.port.out.AccessScope;
import br.com.erpcore.application.support.ApplicationFixture;
import br.com.erpcore.domain.audit.AuditEvent;
import br.com.erpcore.domain.audit.AuditEventType;
import br.com.erpcore.domain.audit.AuditOutcome;
import br.com.erpcore.domain.company.CompanyId;
import br.com.erpcore.domain.identity.AccessDeniedException;
import br.com.erpcore.domain.identity.UserId;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetCurrentContextUseCaseTest {

    private final ApplicationFixture fixture = new ApplicationFixture();
    private final RegisterCompanyAndOwnerResult owner = fixture.registerOwner();
    private final GetCurrentContextUseCase useCase = fixture.currentContextUseCase();

    private GetCurrentContextQuery asOwnerOf(UUID companyId, UUID userId) {
        return new GetCurrentContextQuery(new AuthenticatedPrincipal(userId, companyId), CORRELATION_ID);
    }

    private RegisterCompanyAndOwnerResult registerSecondCompany() {
        return fixture.registerUseCase().execute(new RegisterCompanyAndOwnerCommand("Oficina Fictícia ME",
                "Beto Fictício", "beto.ficticio@exemplo.test", "outra-senha-sintetica-1", "correlacao-sintetica-2"));
    }

    @Test
    void givenAMemberOfTheCompany_whenReadingTheContext_thenUserCompanyAndRoleAreReturned() {
        CurrentContext context = useCase.execute(asOwnerOf(owner.companyId(), owner.userId()));

        assertThat(context.userId()).isEqualTo(owner.userId());
        assertThat(context.email()).isEqualTo(ApplicationFixture.OWNER_EMAIL);
        assertThat(context.displayName()).isEqualTo("Ana Fictícia");
        assertThat(context.companyId()).isEqualTo(owner.companyId());
        assertThat(context.companyName()).isEqualTo("Padaria Fictícia Ltda");
        assertThat(context.role()).isEqualTo("OWNER");
    }

    @Test
    void givenAMemberOfTheCompany_whenReadingTheContext_thenItRunsInATenantScopeAndIsAudited() {
        useCase.execute(asOwnerOf(owner.companyId(), owner.userId()));

        UserId user = new UserId(owner.userId());
        CompanyId company = new CompanyId(owner.companyId());
        assertThat(fixture.unitOfWork.scopes().get(fixture.unitOfWork.scopes().size() - 1))
                .isEqualTo(new AccessScope.Tenant(user, company));
        AuditEvent event = fixture.auditEvents.last();
        assertThat(event.type()).isEqualTo(AuditEventType.CONTEXT_VIEWED);
        assertThat(event.outcome()).isEqualTo(AuditOutcome.SUCCESS);
        assertThat(event.companyId()).contains(company);
        assertThat(event.actorUserId()).contains(user);
        assertThat(event.correlationId()).isEqualTo(CORRELATION_ID);
    }

    @Test
    void givenAUserOfAnotherCompany_whenReadingThisCompanysContext_thenAccessIsDeniedAndNothingIsRevealed() {
        RegisterCompanyAndOwnerResult other = registerSecondCompany();

        assertThatThrownBy(() -> useCase.execute(asOwnerOf(owner.companyId(), other.userId())))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageNotContaining("Padaria")
                .hasMessageNotContaining(ApplicationFixture.OWNER_EMAIL);
    }

    @Test
    void givenADeniedRead_whenItHappens_thenTheFailureIsAuditedAndCommittedBeforeTheErrorIsRaised() {
        RegisterCompanyAndOwnerResult other = registerSecondCompany();

        assertThatThrownBy(() -> useCase.execute(asOwnerOf(owner.companyId(), other.userId())))
                .isInstanceOf(AccessDeniedException.class);

        AuditEvent event = fixture.auditEvents.last();
        assertThat(event.type()).isEqualTo(AuditEventType.CONTEXT_VIEWED);
        assertThat(event.outcome()).isEqualTo(AuditOutcome.FAILURE);
        assertThat(event.details()).containsEntry("reason", "NOT_A_MEMBER");
        assertThat(event.actorUserId()).contains(new UserId(other.userId()));
        assertThat(fixture.unitOfWork.rollbacks()).isZero();
    }

    @Test
    void givenAnUnknownUser_whenReadingTheContext_thenAccessIsDenied() {
        UUID nobody = UUID.fromString("00000000-0000-0000-0000-00000000dead");

        assertThatThrownBy(() -> useCase.execute(asOwnerOf(owner.companyId(), nobody)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void givenAnUnknownCompany_whenReadingTheContext_thenAccessIsDenied() {
        UUID noCompany = UUID.fromString("00000000-0000-0000-0000-00000000c0de");

        assertThatThrownBy(() -> useCase.execute(asOwnerOf(noCompany, owner.userId())))
                .isInstanceOf(AccessDeniedException.class);
    }
}
