package br.com.erpcore.application.quote;

import static br.com.erpcore.application.support.ApplicationFixture.CORRELATION_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.erpcore.application.profile.UpdateCompanyProfileCommand;
import br.com.erpcore.application.contact.ContactView;
import br.com.erpcore.application.contact.CreateContactCommand;
import br.com.erpcore.application.identity.AuthenticatedPrincipal;
import br.com.erpcore.application.identity.RegisterCompanyAndOwnerResult;
import br.com.erpcore.application.shared.port.out.AccessScope;
import br.com.erpcore.application.support.ApplicationFixture;
import br.com.erpcore.application.vehicle.RegisterVehicleCommand;
import br.com.erpcore.application.vehicle.VehicleView;
import br.com.erpcore.domain.audit.AuditEvent;
import br.com.erpcore.domain.audit.AuditEventType;
import br.com.erpcore.domain.company.CompanyId;
import br.com.erpcore.domain.contact.ContactNotFoundException;
import br.com.erpcore.domain.identity.AccessDeniedException;
import br.com.erpcore.domain.identity.TokenHash;
import br.com.erpcore.domain.identity.UserId;
import br.com.erpcore.domain.quote.InvalidQuoteTransitionException;
import br.com.erpcore.domain.quote.QuoteLink;
import br.com.erpcore.domain.quote.QuoteNotFoundException;
import br.com.erpcore.domain.quote.QuoteStatus;
import br.com.erpcore.domain.shared.DomainValidationException;
import br.com.erpcore.domain.vehicle.VehicleNotFoundException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OwnerQuoteUseCasesTest extends QuoteScenario {

    private static final Instant EXPIRES_AFTER_SEVEN_DAYS = Instant.parse("2026-04-09T03:00:00Z");

    // ---------------------------------------------------------------- rascunho

    @Test
    void givenValidData_whenCreatingADraft_thenTheTotalIsCalculatedByTheDomainAndTheViewShowsTheClientAndTheVehicle() {
        QuoteView view = draft();

        assertThat(view.status()).isEqualTo("DRAFT");
        assertThat(view.total()).isEqualTo(ITEMS_TOTAL);
        assertThat(view.items()).extracting(QuoteItemView::lineTotal).containsExactly("179.80", "120.00");
        assertThat(view.items().get(0)).extracting(QuoteItemView::description, QuoteItemView::kind, QuoteItemView::quantity, QuoteItemView::unitPrice)
                .containsExactly("Pastilha de freio", "PART", "2.000", "89.90");
        assertThat(view.contactName()).isEqualTo("Cliente Fictício");
        assertThat(view.contactPhoneMasked()).isEqualTo("(11) *****-4321");
        assertThat(view.vehicleLabel()).isEqualTo("Fiat Uno 2012 · ****234");
        assertThat(view.note()).isEqualTo("Barulho ao frear");
        assertThat(view.sentAt()).isNull();
        assertThat(view.expiresAt()).isNull();
    }

    @Test
    void givenADraftCreated_whenReadingTheAudit_thenItIsRecordedWithoutPersonalDataAndInATenantScope() {
        QuoteView view = draft();

        AuditEvent event = fixture.auditEvents.last();
        assertThat(event.type()).isEqualTo(AuditEventType.QUOTE_DRAFT_CREATED);
        assertThat(event.details()).containsEntry("quoteId", view.id().toString());
        assertThat(event.details().toString()).doesNotContain("Cliente", "987654321", "ABC");
        assertThat(fixture.unitOfWork.scopes().get(fixture.unitOfWork.scopes().size() - 1))
                .isEqualTo(new AccessScope.Tenant(new UserId(owner.userId()), new CompanyId(owner.companyId())));
    }

    @Test
    void givenNoValidity_whenCreatingADraft_thenItUsesTheCompanysDefaultOrSevenDays() {
        assertThat(draft().validityDays()).isEqualTo(7);

        fixture.updateCompanyProfileUseCase().execute(new UpdateCompanyProfileCommand(principal, "Oficina", null, null, null, 10, null, CORRELATION_ID));

        assertThat(draft().validityDays()).isEqualTo(10);
        assertThat(draft(principal, client.id(), car.id(), items(), 3).validityDays()).isEqualTo(3);
    }

    @Test
    void givenNoItems_whenCreatingADraft_thenTheDraftIsEmptyAndTheTotalIsZero() {
        QuoteView view = draft(principal, client.id(), car.id(), List.of(), null);

        assertThat(view.items()).isEmpty();
        assertThat(view.total()).isEqualTo("0.00");
    }

    @Test
    void givenAContactOrVehicleThatDoesNotFit_whenCreatingADraft_thenItIsNotFoundAndNothingIsSaved() {
        AuthenticatedPrincipal other = fixture.principalOf(fixture.registerOtherOwner());
        ContactView otherClient = fixture.createContactUseCase().execute(new CreateContactCommand(other, "Cliente de outra oficina", "11987654399", null, CORRELATION_ID));
        VehicleView otherCar = fixture.registerVehicleUseCase().execute(new RegisterVehicleCommand(other, otherClient.id(), "QWE1R23", "VW", "Gol", 2020, null, CORRELATION_ID));
        ContactView secondClient = fixture.createContactUseCase().execute(new CreateContactCommand(principal, "Outro Cliente", "11987654322", null, CORRELATION_ID));

        assertThatThrownBy(() -> draft(principal, UUID.randomUUID(), car.id(), items(), null)).isInstanceOf(ContactNotFoundException.class);
        assertThatThrownBy(() -> draft(principal, otherClient.id(), car.id(), items(), null)).isInstanceOf(ContactNotFoundException.class);
        assertThatThrownBy(() -> draft(principal, client.id(), UUID.randomUUID(), items(), null)).isInstanceOf(VehicleNotFoundException.class);
        assertThatThrownBy(() -> draft(principal, client.id(), otherCar.id(), items(), null)).isInstanceOf(VehicleNotFoundException.class);
        assertThatThrownBy(() -> draft(principal, secondClient.id(), car.id(), items(), null)).isInstanceOf(VehicleNotFoundException.class);
        assertThat(storedQuotesCount()).isZero();
    }

    @Test
    void givenAnInvalidItem_whenCreatingADraft_thenTheErrorNamesTheItemAndTheFieldAndNothingIsSaved() {
        List<QuoteItemInput> bad = List.of(new QuoteItemInput("Pastilha", "PART", "2", "89.90"),
                new QuoteItemInput("Mão de obra", "LABOR", "abc", "80.00"));

        assertThatThrownBy(() -> draft(principal, client.id(), car.id(), bad, null))
                .isInstanceOfSatisfying(DomainValidationException.class, e -> {
                    assertThat(e.field()).isEqualTo("items[1].quantity");
                    assertThat(e.code()).isEqualTo("INVALID_FORMAT");
                });
        assertThatThrownBy(() -> draft(principal, client.id(), car.id(), List.of(new QuoteItemInput("x", "OUTRO", "1", "1")), null))
                .isInstanceOfSatisfying(DomainValidationException.class, e -> assertThat(e.field()).isEqualTo("items[0].kind"));
        assertThatThrownBy(() -> draft(principal, client.id(), car.id(), List.of(new QuoteItemInput(" ", "PART", "1", "1")), null))
                .isInstanceOfSatisfying(DomainValidationException.class, e -> assertThat(e.field()).isEqualTo("items[0].description"));
        assertThatThrownBy(() -> draft(principal, client.id(), car.id(), List.of(new QuoteItemInput("x", "PART", "1", "-5")), null))
                .isInstanceOfSatisfying(DomainValidationException.class, e -> assertThat(e.field()).isEqualTo("items[0].unitPrice"));
        assertThat(storedQuotesCount()).isZero();
    }

    @Test
    void givenNullItemsOrANullKind_whenCreatingADraft_thenNullItemsMeanNoItemsAndANullKindIsInvalid() {
        assertThat(draft(principal, client.id(), car.id(), null, null).items()).isEmpty();

        assertThatThrownBy(() -> draft(principal, client.id(), car.id(), List.of(new QuoteItemInput("x", null, "1", "1")), null))
                .isInstanceOfSatisfying(DomainValidationException.class, e -> assertThat(e.field()).isEqualTo("items[0].kind"));
    }

    // ---------------------------------------------------------------- revisão dos itens

    @Test
    void givenADraft_whenRevisingTheItems_thenTheNewItemsAndTotalReplaceTheOldOnesAndItIsAudited() {
        QuoteView created = draft();

        QuoteView revised = fixture.reviseQuoteItemsUseCase().execute(new ReviseQuoteItemsCommand(principal, created.id(),
                List.of(new QuoteItemInput("Só esta", "PART", "1", "10")), CORRELATION_ID));

        assertThat(revised.items()).extracting(QuoteItemView::description).containsExactly("Só esta");
        assertThat(revised.total()).isEqualTo("10.00");
        assertThat(get(created.id()).total()).isEqualTo("10.00");
        assertThat(fixture.auditEvents.last().type()).isEqualTo(AuditEventType.QUOTE_ITEMS_REVISED);
    }

    @Test
    void givenAQuoteThatWasAlreadySent_whenRevisingTheItems_thenTheTransitionIsRefusedAndTheItemsStay() {
        QuoteSentResult sent = sendNew();

        assertThatThrownBy(() -> fixture.reviseQuoteItemsUseCase().execute(new ReviseQuoteItemsCommand(principal, sent.quoteId(),
                List.of(new QuoteItemInput("x", "PART", "1", "1")), CORRELATION_ID))).isInstanceOf(InvalidQuoteTransitionException.class);
        assertThat(get(sent.quoteId()).items()).hasSize(2);
    }

    @Test
    void givenAQuoteThatDoesNotExistOrBelongsToAnotherCompany_whenRevisingOrReading_thenItIsNotFound() {
        AuthenticatedPrincipal other = fixture.principalOf(fixture.registerOtherOwner());
        UUID mine = draft().id();

        assertThatThrownBy(() -> fixture.reviseQuoteItemsUseCase().execute(new ReviseQuoteItemsCommand(other, mine, items(), CORRELATION_ID)))
                .isInstanceOf(QuoteNotFoundException.class);
        assertThatThrownBy(() -> fixture.getQuoteUseCase().execute(new GetQuoteQuery(other, mine))).isInstanceOf(QuoteNotFoundException.class);
        assertThatThrownBy(() -> get(UUID.randomUUID())).isInstanceOf(QuoteNotFoundException.class);
    }

    // ---------------------------------------------------------------- confirmar e enviar

    @Test
    void givenADraftWithItems_whenTheOwnerConfirms_thenItIsSentWithTheLinkAndTheValidityUntilTheEndOfTheDay() {
        QuoteView created = draft();

        QuoteSentResult sent = confirm(created.id());

        assertThat(sent.quoteId()).isEqualTo(created.id());
        assertThat(sent.expiresAt()).isEqualTo(EXPIRES_AFTER_SEVEN_DAYS);
        assertThat(sent.publicUrl()).startsWith("https://oficina.exemplo.test/o/");
        assertThat(sent.delivered()).isTrue();
        QuoteView view = get(created.id());
        assertThat(view.status()).isEqualTo("SENT");
        assertThat(view.sentAt()).isEqualTo(ApplicationFixture.START);
        assertThat(view.expiresAt()).isEqualTo(EXPIRES_AFTER_SEVEN_DAYS);
    }

    @Test
    void givenAConfirmedQuote_whenLookingAtWhatWasStored_thenOnlyTheHashOfTheTokenIsThere() {
        QuoteSentResult sent = sendNew();
        String token = tokenOf(sent);

        List<QuoteLink> links = fixture.quoteLinks.all();

        assertThat(links).hasSize(1);
        assertThat(links.get(0).tokenHash()).isEqualTo(new TokenHash(fixture.quoteLinkCodec.hash(token)));
        assertThat(links.get(0).tokenHash().value()).isNotEqualTo(token);
        assertThat(links.get(0).quoteId().value()).isEqualTo(sent.quoteId());
        assertThat(links.get(0).expiresAt()).isEqualTo(sent.expiresAt());
        assertThat(links.get(0).decidedAt()).isEmpty();
    }

    @Test
    void givenAConfirmedQuote_whenReadingTheAuditAndTheDelivery_thenTheSendIsRecordedAndDeliveredToTheContact() {
        QuoteSentResult sent = sendNew();

        AuditEvent event = fixture.auditEvents.last();
        assertThat(event.type()).isEqualTo(AuditEventType.QUOTE_SENT);
        assertThat(event.details()).containsEntry("quoteId", sent.quoteId().toString());
        assertThat(event.details().toString()).doesNotContain(tokenOf(sent));
        assertThat(fixture.quoteDelivery.delivered()).hasSize(1);
        assertThat(fixture.quoteDelivery.last().contactId().value()).isEqualTo(client.id());
        assertThat(fixture.quoteDelivery.last().publicUrl()).isEqualTo(sent.publicUrl());
    }

    @Test
    void givenADeliveryChannelThatFails_whenConfirming_thenTheQuoteIsStillSentAndTheResultSaysItWasNotDelivered() {
        fixture.quoteDelivery.failFromNowOn();

        QuoteSentResult sent = sendNew();

        assertThat(sent.delivered()).isFalse();
        assertThat(get(sent.quoteId()).status()).isEqualTo("SENT");
        assertThat(sent.publicUrl()).isNotBlank();
    }

    @Test
    void givenAQuoteWaitingForTheOwner_whenTheOwnerConfirms_thenItIsSentToo() {
        UUID id = draft().id();
        requestOwnerConfirmation(id);
        assertThat(get(id).status()).isEqualTo("AWAITING_OWNER_CONFIRMATION");

        confirm(id);

        assertThat(get(id).status()).isEqualTo("SENT");
    }

    @Test
    void givenAQuoteAlreadySent_whenConfirmingAgain_thenItIsAnInvalidTransitionWithoutANewLinkOrDelivery() {
        QuoteSentResult sent = sendNew();

        assertThatThrownBy(() -> confirm(sent.quoteId())).isInstanceOf(InvalidQuoteTransitionException.class);

        assertThat(fixture.quoteLinks.all()).hasSize(1);
        assertThat(fixture.quoteDelivery.delivered()).hasSize(1);
    }

    @Test
    void givenADraftWithoutItems_whenConfirming_thenItIsNotReadyAndStaysADraft() {
        UUID id = draft(principal, client.id(), car.id(), List.of(), null).id();

        assertThatThrownBy(() -> confirm(id)).isInstanceOfSatisfying(DomainValidationException.class, e -> assertThat(e.field()).isEqualTo("items"));

        assertThat(get(id).status()).isEqualTo("DRAFT");
        assertThat(fixture.quoteLinks.all()).isEmpty();
        assertThat(fixture.quoteDelivery.delivered()).isEmpty();
    }

    // ---------------------------------------------------------------- reenviar

    @Test
    void givenASentQuote_whenResending_thenANewLinkIsIssuedAndDeliveredAndTheStateDoesNotChange() {
        QuoteSentResult first = sendNew();

        QuoteSentResult second = fixture.sendQuoteUseCase().execute(new SendQuoteCommand(principal, first.quoteId(), CORRELATION_ID));

        assertThat(second.publicUrl()).isNotEqualTo(first.publicUrl());
        assertThat(second.expiresAt()).isEqualTo(first.expiresAt());
        assertThat(get(first.quoteId()).status()).isEqualTo("SENT");
        assertThat(fixture.quoteLinks.all()).hasSize(2);
        assertThat(fixture.quoteDelivery.delivered()).hasSize(2);
        assertThat(fixture.auditEvents.last().type()).isEqualTo(AuditEventType.QUOTE_RESENT);
    }

    @Test
    void givenAQuoteThatIsNotSentOrIsPastItsDeadline_whenResending_thenItIsAnInvalidTransition() {
        UUID draftId = draft().id();
        QuoteSentResult sent = sendNew();
        fixture.clock.advance(Duration.ofDays(8));

        assertThatThrownBy(() -> fixture.sendQuoteUseCase().execute(new SendQuoteCommand(principal, draftId, CORRELATION_ID)))
                .isInstanceOf(InvalidQuoteTransitionException.class);
        assertThatThrownBy(() -> fixture.sendQuoteUseCase().execute(new SendQuoteCommand(principal, sent.quoteId(), CORRELATION_ID)))
                .isInstanceOf(InvalidQuoteTransitionException.class);
    }

    // ---------------------------------------------------------------- cancelar

    @Test
    void givenASentQuote_whenCancelling_thenItIsCancelledAndAudited() {
        QuoteSentResult sent = sendNew();

        QuoteView cancelled = fixture.cancelQuoteUseCase().execute(new CancelQuoteCommand(principal, sent.quoteId(), CORRELATION_ID));

        assertThat(cancelled.status()).isEqualTo("CANCELLED");
        assertThat(fixture.auditEvents.last().type()).isEqualTo(AuditEventType.QUOTE_CANCELLED);
    }

    @Test
    void givenACancelledQuote_whenCancellingAgain_thenNothingChangesAndNoSecondAuditEventIsWritten() {
        QuoteSentResult sent = sendNew();
        fixture.cancelQuoteUseCase().execute(new CancelQuoteCommand(principal, sent.quoteId(), CORRELATION_ID));
        int events = fixture.auditEvents.all().size();

        QuoteView again = fixture.cancelQuoteUseCase().execute(new CancelQuoteCommand(principal, sent.quoteId(), CORRELATION_ID));

        assertThat(again.status()).isEqualTo("CANCELLED");
        assertThat(fixture.auditEvents.all()).hasSize(events);
    }

    // ---------------------------------------------------------------- leitura e fila

    @Test
    void givenASentQuotePastItsDeadlineBeforeAnyJobRuns_whenReadingIt_thenItAlreadyShowsAsExpired() {
        QuoteSentResult sent = sendNew();

        fixture.clock.advance(Duration.ofDays(8));

        assertThat(get(sent.quoteId()).status()).isEqualTo("EXPIRED");
    }

    @Test
    void givenQuotesInSeveralStates_whenListingTheQueue_thenTheOwnerSeesWhatNeedsHimFirstThenWhatWaitsForTheClientThenTheClosedOnes() {
        UUID draftOnly = draft().id();
        UUID waitingOwner = draft().id();
        requestOwnerConfirmation(waitingOwner);
        QuoteSentResult sentOk = sendNew();
        QuoteSentResult sentToExpire = sendNew();
        QuoteSentResult cancelled = sendNew();
        fixture.cancelQuoteUseCase().execute(new CancelQuoteCommand(principal, cancelled.quoteId(), CORRELATION_ID));
        fixture.clock.advance(Duration.ofDays(8));
        // Um novo envio depois do avanço do relógio: este continua válido.
        QuoteSentResult sentFresh = sendNew();

        QuoteQueue queue = fixture.listQuotesUseCase().execute(new ListQuotesQuery(principal));

        assertThat(queue.needsOwner()).extracting(QuoteSummary::id).containsExactlyInAnyOrder(draftOnly, waitingOwner);
        assertThat(queue.awaitingClient()).extracting(QuoteSummary::id).containsExactly(sentFresh.quoteId());
        assertThat(queue.closed()).extracting(QuoteSummary::id).contains(sentOk.quoteId(), sentToExpire.quoteId(), cancelled.quoteId());
        assertThat(queue.closed()).filteredOn(summary -> summary.id().equals(sentOk.quoteId())).extracting(QuoteSummary::status).containsExactly("EXPIRED");
        assertThat(queue.needsOwner().get(0)).extracting(QuoteSummary::contactName, QuoteSummary::vehicleLabel, QuoteSummary::total)
                .containsExactly("Cliente Fictício", "Fiat Uno 2012 · ****234", ITEMS_TOTAL);
    }

    @Test
    void givenTwoCompanies_whenListingTheQueue_thenEachSeesOnlyItsOwnQuotes() {
        AuthenticatedPrincipal other = fixture.principalOf(fixture.registerOtherOwner());
        draft();

        QuoteQueue otherQueue = fixture.listQuotesUseCase().execute(new ListQuotesQuery(other));

        assertThat(otherQueue.needsOwner()).isEmpty();
        assertThat(otherQueue.awaitingClient()).isEmpty();
        assertThat(otherQueue.closed()).isEmpty();
    }

    // ---------------------------------------------------------------- autorização

    @Test
    void givenSomeoneWhoIsNotAMember_whenUsingAnyOwnerUseCase_thenAccessIsDeniedForAllOfThem() {
        UUID quoteId = sendNew().quoteId();
        AuthenticatedPrincipal outsider = new AuthenticatedPrincipal(UUID.randomUUID(), owner.companyId());

        assertThatThrownBy(() -> draft(outsider, client.id(), car.id(), items(), null)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> fixture.reviseQuoteItemsUseCase().execute(new ReviseQuoteItemsCommand(outsider, quoteId, items(), CORRELATION_ID)))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> fixture.confirmQuoteUseCase().execute(new ConfirmQuoteCommand(outsider, quoteId, CORRELATION_ID)))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> fixture.sendQuoteUseCase().execute(new SendQuoteCommand(outsider, quoteId, CORRELATION_ID)))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> fixture.cancelQuoteUseCase().execute(new CancelQuoteCommand(outsider, quoteId, CORRELATION_ID)))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> fixture.getQuoteUseCase().execute(new GetQuoteQuery(outsider, quoteId))).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> fixture.listQuotesUseCase().execute(new ListQuotesQuery(outsider))).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void givenAnOwnerOfAnotherCompany_whenActingOnThisCompanysQuote_thenItIsNotFoundForEveryAction() {
        UUID quoteId = sendNew().quoteId();
        RegisterCompanyAndOwnerResult otherOwner = fixture.registerOtherOwner();
        AuthenticatedPrincipal other = fixture.principalOf(otherOwner);

        assertThatThrownBy(() -> fixture.confirmQuoteUseCase().execute(new ConfirmQuoteCommand(other, quoteId, CORRELATION_ID)))
                .isInstanceOf(QuoteNotFoundException.class);
        assertThatThrownBy(() -> fixture.sendQuoteUseCase().execute(new SendQuoteCommand(other, quoteId, CORRELATION_ID)))
                .isInstanceOf(QuoteNotFoundException.class);
        assertThatThrownBy(() -> fixture.cancelQuoteUseCase().execute(new CancelQuoteCommand(other, quoteId, CORRELATION_ID)))
                .isInstanceOf(QuoteNotFoundException.class);
        assertThat(get(quoteId).status()).isEqualTo("SENT");
    }
}
