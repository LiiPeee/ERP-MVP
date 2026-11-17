package br.com.erpcore.application.quote;

import static br.com.erpcore.application.support.ApplicationFixture.CORRELATION_ID;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.erpcore.application.contact.CreateContactCommand;
import br.com.erpcore.application.identity.AuthenticatedPrincipal;
import br.com.erpcore.application.shared.port.out.AccessScope;
import br.com.erpcore.application.vehicle.RegisterVehicleCommand;
import br.com.erpcore.domain.audit.AuditEvent;
import br.com.erpcore.domain.audit.AuditEventType;
import br.com.erpcore.domain.audit.AuditOutcome;
import br.com.erpcore.domain.company.CompanyId;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ExpireDueQuotesUseCaseTest extends QuoteScenario {

    private final AuthenticatedPrincipal other = fixture.principalOf(fixture.registerOtherOwner());

    private QuoteSentResult sendFromOtherCompany() {
        UUID otherClient = fixture.createContactUseCase().execute(new CreateContactCommand(other, "Cliente de outra oficina", "11987654399", null, CORRELATION_ID)).id();
        UUID otherCar = fixture.registerVehicleUseCase().execute(new RegisterVehicleCommand(other, otherClient, "QWE1R23", "VW", "Gol", 2020, null, CORRELATION_ID)).id();
        UUID otherDraft = draft(other, otherClient, otherCar, items(), null).id();
        return fixture.confirmQuoteUseCase().execute(new ConfirmQuoteCommand(other, otherDraft, CORRELATION_ID));
    }

    private String storedStatus(AuthenticatedPrincipal who, UUID quoteId) {
        // O estado gravado, sem a leitura derivada pelo relógio.
        return fixture.unitOfWork.execute(new AccessScope.Anonymous(),
                () -> fixture.quotes.findById(new CompanyId(who.companyId()), new br.com.erpcore.domain.quote.QuoteId(quoteId)).orElseThrow().status().name());
    }

    @Test
    void givenSentQuotesPastTheirDeadlineInSeveralCompanies_whenTheJobRuns_thenOnlyThoseAreExpiredAndEachIsAuditedWithoutAnActor() {
        QuoteSentResult mine = sendNew();
        QuoteSentResult theirs = sendFromOtherCompany();
        fixture.clock.advance(Duration.ofDays(8));
        QuoteSentResult fresh = sendNew();
        int eventsBefore = fixture.auditEvents.all().size();

        int expired = fixture.expireDueQuotesUseCase(10).execute(CORRELATION_ID);

        assertThat(expired).isEqualTo(2);
        assertThat(storedStatus(principal, mine.quoteId())).isEqualTo("EXPIRED");
        assertThat(storedStatus(other, theirs.quoteId())).isEqualTo("EXPIRED");
        assertThat(storedStatus(principal, fresh.quoteId())).isEqualTo("SENT");
        List<AuditEvent> events = fixture.auditEvents.all().subList(eventsBefore, fixture.auditEvents.all().size());
        assertThat(events).hasSize(2).allSatisfy(event -> {
            assertThat(event.type()).isEqualTo(AuditEventType.QUOTE_EXPIRED);
            assertThat(event.outcome()).isEqualTo(AuditOutcome.SUCCESS);
            assertThat(event.actorUserId()).isEmpty();
            assertThat(event.correlationId()).isEqualTo(CORRELATION_ID);
        });
        assertThat(events).extracting(event -> event.companyId().orElseThrow()).containsExactlyInAnyOrder(
                new CompanyId(owner.companyId()), new CompanyId(other.companyId()));
        assertThat(events).extracting(event -> event.details().get("quoteId")).containsExactlyInAnyOrder(
                mine.quoteId().toString(), theirs.quoteId().toString());
    }

    @Test
    void givenTheJob_whenItRuns_thenItUsesTheNarrowExpirationScopeAndNothingElse() {
        sendNew();
        fixture.clock.advance(Duration.ofDays(8));
        int before = fixture.unitOfWork.scopes().size();

        fixture.expireDueQuotesUseCase(10).execute(CORRELATION_ID);

        assertThat(fixture.unitOfWork.scopes().subList(before, fixture.unitOfWork.scopes().size()))
                .allSatisfy(scope -> assertThat(scope).isEqualTo(new AccessScope.QuoteExpiration()));
    }

    @Test
    void givenTheJobAlreadyRan_whenItRunsAgain_thenNothingIsExpiredAndNothingIsAudited() {
        sendNew();
        fixture.clock.advance(Duration.ofDays(8));
        fixture.expireDueQuotesUseCase(10).execute(CORRELATION_ID);
        int events = fixture.auditEvents.all().size();

        int expired = fixture.expireDueQuotesUseCase(10).execute(CORRELATION_ID);

        assertThat(expired).isZero();
        assertThat(fixture.auditEvents.all()).hasSize(events);
    }

    @Test
    void givenNothingIsDue_whenTheJobRuns_thenNothingChanges() {
        QuoteSentResult sent = sendNew();

        assertThat(fixture.expireDueQuotesUseCase(10).execute(CORRELATION_ID)).isZero();
        assertThat(storedStatus(principal, sent.quoteId())).isEqualTo("SENT");
    }

    @Test
    void givenMoreDueQuotesThanOneBatch_whenTheJobRuns_thenItKeepsGoingUntilAllAreExpired() {
        List<QuoteSentResult> sent = List.of(sendNew(), sendNew(), sendNew(), sendNew(), sendNew());
        fixture.clock.advance(Duration.ofDays(8));

        int expired = fixture.expireDueQuotesUseCase(2).execute(CORRELATION_ID);

        assertThat(expired).isEqualTo(5);
        assertThat(sent).allSatisfy(result -> assertThat(storedStatus(principal, result.quoteId())).isEqualTo("EXPIRED"));
    }

    @Test
    void givenQuotesThatAreNotSent_whenTheJobRuns_thenTheyAreLeftAlone() {
        UUID draftId = draft().id();
        QuoteSentResult toCancel = sendNew();
        fixture.cancelQuoteUseCase().execute(new CancelQuoteCommand(principal, toCancel.quoteId(), CORRELATION_ID));
        fixture.clock.advance(Duration.ofDays(8));

        assertThat(fixture.expireDueQuotesUseCase(10).execute(CORRELATION_ID)).isZero();
        assertThat(storedStatus(principal, draftId)).isEqualTo("DRAFT");
        assertThat(storedStatus(principal, toCancel.quoteId())).isEqualTo("CANCELLED");
    }
}
