package br.com.erpcore.application.quote;

import static br.com.erpcore.application.support.ApplicationFixture.CORRELATION_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.erpcore.application.contact.CreateContactCommand;
import br.com.erpcore.application.identity.AuthenticatedPrincipal;
import br.com.erpcore.application.shared.port.out.AccessScope;
import br.com.erpcore.application.support.ApplicationFixture;
import br.com.erpcore.application.vehicle.RegisterVehicleCommand;
import br.com.erpcore.domain.audit.AuditEvent;
import br.com.erpcore.domain.audit.AuditEventType;
import br.com.erpcore.domain.audit.AuditOutcome;
import br.com.erpcore.domain.company.CompanyId;
import br.com.erpcore.domain.identity.TokenHash;
import br.com.erpcore.domain.quote.QuoteAlreadyDecidedException;
import br.com.erpcore.domain.quote.QuoteId;
import br.com.erpcore.domain.quote.QuoteLinkUnavailableException;
import br.com.erpcore.domain.shared.DomainValidationException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PublicQuoteUseCasesTest extends QuoteScenario {

    private static final String PRIVATE_DATA_PHONE = "987654321";
    private static final String PRIVATE_DATA_PLATE = "ABC1234";

    private PublicQuoteView view(String token) {
        return fixture.getPublicQuoteUseCase().execute(new GetPublicQuoteQuery(token));
    }

    private PublicQuoteDecision approve(String token) {
        return fixture.approveQuoteByLinkUseCase().execute(new DecideQuoteByLinkCommand(token, null, CORRELATION_ID));
    }

    private PublicQuoteDecision reject(String token, String reason) {
        return fixture.rejectQuoteByLinkUseCase().execute(new DecideQuoteByLinkCommand(token, reason, CORRELATION_ID));
    }

    // ---------------------------------------------------------------- consulta

    @Test
    void givenAValidLink_whenOpeningIt_thenOnlyTheMinimumIsShownWithoutPhoneOrFullPlate() {
        fixture.updateCompanyProfileUseCase().execute(new br.com.erpcore.application.profile.UpdateCompanyProfileCommand(principal,
                "Oficina do Zé Fictício", "11933334444", null, null, 7, null, CORRELATION_ID));
        QuoteSentResult sent = sendNew();

        PublicQuoteView view = view(tokenOf(sent));

        assertThat(view.shopName()).isEqualTo("Oficina do Zé Fictício");
        assertThat(view.vehicleLabel()).isEqualTo("Fiat Uno 2012 · placa final 234");
        assertThat(view.items()).extracting(QuoteItemView::description).containsExactly("Pastilha de freio", "Mão de obra");
        assertThat(view.total()).isEqualTo(ITEMS_TOTAL);
        assertThat(view.status()).isEqualTo("SENT");
        assertThat(view.expiresAt()).isEqualTo(sent.expiresAt());
        assertThat(view.toString()).doesNotContain(PRIVATE_DATA_PHONE, PRIVATE_DATA_PLATE, "Cliente Fictício", "11933334444");
    }

    @Test
    void givenAValidLink_whenOpeningItManyTimes_thenNothingIsConsumedAndTheClientCanStillDecide() {
        QuoteSentResult sent = sendNew();
        String token = tokenOf(sent);

        view(token);
        view(token);

        assertThat(fixture.quoteLinks.all().get(0).decidedAt()).isEmpty();
        assertThat(approve(token).status()).isEqualTo("APPROVED");
    }

    @Test
    void givenALinkThatDoesNotExistIsExpiredOrBelongsToACancelledQuote_whenOpeningIt_thenItIsAlwaysTheSameUnavailableAnswer() {
        QuoteSentResult sent = sendNew();
        QuoteSentResult toCancel = sendNew();
        fixture.cancelQuoteUseCase().execute(new CancelQuoteCommand(principal, toCancel.quoteId(), CORRELATION_ID));

        assertThatThrownBy(() -> view("token-que-nao-existe")).isExactlyInstanceOf(QuoteLinkUnavailableException.class);
        assertThatThrownBy(() -> view("")).isExactlyInstanceOf(QuoteLinkUnavailableException.class);
        assertThatThrownBy(() -> view(null)).isExactlyInstanceOf(QuoteLinkUnavailableException.class);
        assertThatThrownBy(() -> view(tokenOf(toCancel))).isExactlyInstanceOf(QuoteLinkUnavailableException.class);
        fixture.clock.advance(Duration.ofDays(8));
        assertThatThrownBy(() -> view(tokenOf(sent))).isExactlyInstanceOf(QuoteLinkUnavailableException.class);
    }

    @Test
    void givenTheLookup_whenOpeningALink_thenItFirstRunsUnderTheLinkScopeAndThenUnderAScopeBoundToThatSingleQuote() {
        QuoteSentResult sent = sendNew();
        int before = fixture.unitOfWork.scopes().size();

        view(tokenOf(sent));

        List<AccessScope> used = fixture.unitOfWork.scopes().subList(before, fixture.unitOfWork.scopes().size());
        assertThat(used).containsExactly(
                new AccessScope.QuoteLinkLookup(new TokenHash(fixture.quoteLinkCodec.hash(tokenOf(sent)))),
                new AccessScope.QuoteDecision(new CompanyId(owner.companyId()), new QuoteId(sent.quoteId())));
    }

    // ---------------------------------------------------------------- decisão

    @Test
    void givenASentQuote_whenTheClientApproves_thenItIsApprovedTheLinkIsConsumedAndItIsAuditedWithoutAnActor() {
        QuoteSentResult sent = sendNew();

        PublicQuoteDecision decision = approve(tokenOf(sent));

        assertThat(decision.status()).isEqualTo("APPROVED");
        assertThat(get(sent.quoteId()).status()).isEqualTo("APPROVED");
        assertThat(fixture.quoteLinks.all().get(0).decidedAt()).hasValue(ApplicationFixture.START);
        AuditEvent event = fixture.auditEvents.last();
        assertThat(event.type()).isEqualTo(AuditEventType.QUOTE_APPROVED_BY_LINK);
        assertThat(event.outcome()).isEqualTo(AuditOutcome.SUCCESS);
        assertThat(event.actorUserId()).isEmpty();
        assertThat(event.companyId()).contains(new CompanyId(owner.companyId()));
        assertThat(event.details()).containsEntry("quoteId", sent.quoteId().toString());
        assertThat(event.details().toString()).doesNotContain(tokenOf(sent));
    }

    @Test
    void givenADecisionOnTheLink_whenTheQuoteIsRead_thenItWasReadWithALockSoTwoDecisionsQueueUp() {
        QuoteSentResult sent = sendNew();

        approve(tokenOf(sent));

        assertThat(fixture.quotes.lockedReads()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void givenASentQuote_whenTheClientRejectsWithAReason_thenItIsRejectedAndTheReasonIsKept() {
        QuoteSentResult sent = sendNew();

        PublicQuoteDecision decision = reject(tokenOf(sent), "  Achei caro ");

        assertThat(decision.status()).isEqualTo("REJECTED");
        QuoteView view = get(sent.quoteId());
        assertThat(view.status()).isEqualTo("REJECTED");
        assertThat(view.decisionReason()).isEqualTo("Achei caro");
        assertThat(fixture.auditEvents.last().type()).isEqualTo(AuditEventType.QUOTE_REJECTED_BY_LINK);
        assertThat(fixture.auditEvents.last().details().toString()).doesNotContain("caro");
    }

    @Test
    void givenARejectionWithoutReason_whenTheClientRejects_thenThereIsNoReason() {
        QuoteSentResult sent = sendNew();

        reject(tokenOf(sent), null);

        assertThat(get(sent.quoteId()).decisionReason()).isNull();
    }

    @Test
    void givenTheSameDecisionRepeated_whenTheClientDoubleClicks_thenNothingChangesAndNoSecondAuditEventIsWritten() {
        QuoteSentResult sent = sendNew();
        approve(tokenOf(sent));
        int events = fixture.auditEvents.all().size();

        PublicQuoteDecision again = approve(tokenOf(sent));

        assertThat(again.status()).isEqualTo("APPROVED");
        assertThat(fixture.auditEvents.all()).hasSize(events);
    }

    @Test
    void givenADecisionAlreadyTaken_whenTheOppositeArrives_thenItIsRefusedAsAlreadyDecided() {
        QuoteSentResult approved = sendNew();
        QuoteSentResult rejected = sendNew();
        approve(tokenOf(approved));
        reject(tokenOf(rejected), null);

        assertThatThrownBy(() -> reject(tokenOf(approved), null)).isInstanceOf(QuoteAlreadyDecidedException.class);
        assertThatThrownBy(() -> approve(tokenOf(rejected))).isInstanceOf(QuoteAlreadyDecidedException.class);
        assertThat(get(approved.quoteId()).status()).isEqualTo("APPROVED");
        assertThat(get(rejected.quoteId()).status()).isEqualTo("REJECTED");
    }

    @Test
    void givenAReasonThatIsTooLong_whenRejecting_thenItIsRejectedAndTheLinkStaysUsable() {
        QuoteSentResult sent = sendNew();

        assertThatThrownBy(() -> reject(tokenOf(sent), "a".repeat(301)))
                .isInstanceOfSatisfying(DomainValidationException.class, e -> assertThat(e.field()).isEqualTo("reason"));

        assertThat(get(sent.quoteId()).status()).isEqualTo("SENT");
        assertThat(approve(tokenOf(sent)).status()).isEqualTo("APPROVED");
    }

    @Test
    void givenAnUnknownExpiredOrCancelledLink_whenTheClientDecides_thenItIsTheSameUnavailableAnswerAndNothingChanges() {
        QuoteSentResult expiring = sendNew();
        QuoteSentResult cancelled = sendNew();
        fixture.cancelQuoteUseCase().execute(new CancelQuoteCommand(principal, cancelled.quoteId(), CORRELATION_ID));

        assertThatThrownBy(() -> approve("token-que-nao-existe")).isExactlyInstanceOf(QuoteLinkUnavailableException.class);
        assertThatThrownBy(() -> reject(tokenOf(cancelled), null)).isExactlyInstanceOf(QuoteLinkUnavailableException.class);
        fixture.clock.advance(Duration.ofDays(8));
        assertThatThrownBy(() -> approve(tokenOf(expiring))).isExactlyInstanceOf(QuoteLinkUnavailableException.class);
        assertThat(get(cancelled.quoteId()).status()).isEqualTo("CANCELLED");
        assertThat(get(expiring.quoteId()).status()).isEqualTo("EXPIRED");
    }

    @Test
    void givenALinkThatOutlivesItsQuote_whenTheClientDecidesAfterTheQuoteDeadline_thenTheDomainStillRefusesItAsUnavailable() {
        QuoteSentResult sent = sendNew();
        // Defesa em profundidade: mesmo um link com validade maior que a do orçamento não deixa decidir depois do prazo.
        fixture.unitOfWork.execute(new AccessScope.Anonymous(), () -> {
            TokenHash hash = new TokenHash(fixture.quoteLinkCodec.hash(tokenOf(sent)));
            br.com.erpcore.domain.quote.QuoteLink original = fixture.quoteLinks.findByHash(hash).orElseThrow();
            fixture.quoteLinks.save(br.com.erpcore.domain.quote.QuoteLink.restore(original.quoteId(), original.companyId(), hash,
                    original.issuedAt(), original.expiresAt().plus(Duration.ofDays(30)), null));
            return null;
        });
        fixture.clock.advance(Duration.ofDays(8));

        assertThatThrownBy(() -> approve(tokenOf(sent))).isExactlyInstanceOf(QuoteLinkUnavailableException.class);
        assertThat(get(sent.quoteId()).status()).isEqualTo("EXPIRED");
    }

    @Test
    void givenADecidedQuote_whenOpeningTheLinkAgain_thenTheResultIsShownUntilTheDeadline() {
        QuoteSentResult sent = sendNew();
        reject(tokenOf(sent), "Achei caro");

        assertThat(view(tokenOf(sent)).status()).isEqualTo("REJECTED");

        fixture.clock.advance(Duration.ofDays(8));
        assertThatThrownBy(() -> view(tokenOf(sent))).isExactlyInstanceOf(QuoteLinkUnavailableException.class);
    }

    @Test
    void givenTwoCompanies_whenOneClientDecidesWithItsLink_thenOnlyThatCompanysQuoteChanges() {
        QuoteSentResult mine = sendNew();
        AuthenticatedPrincipal other = fixture.principalOf(fixture.registerOtherOwner());
        UUID otherClient = fixture.createContactUseCase().execute(new CreateContactCommand(other, "Cliente de outra oficina", "11987654399", null, CORRELATION_ID)).id();
        UUID otherCar = fixture.registerVehicleUseCase().execute(new RegisterVehicleCommand(other, otherClient, "QWE1R23", "VW", "Gol", 2020, null, CORRELATION_ID)).id();
        UUID otherDraft = draft(other, otherClient, otherCar, items(), null).id();
        QuoteSentResult theirs = fixture.confirmQuoteUseCase().execute(new ConfirmQuoteCommand(other, otherDraft, CORRELATION_ID));

        approve(tokenOf(mine));

        assertThat(get(mine.quoteId()).status()).isEqualTo("APPROVED");
        assertThat(fixture.getQuoteUseCase().execute(new GetQuoteQuery(other, theirs.quoteId())).status()).isEqualTo("SENT");
    }
}
