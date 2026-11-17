package br.com.erpcore.domain.quote;

import static br.com.erpcore.domain.shared.DomainValidationAssertions.assertRejectedWith;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.erpcore.domain.company.CompanyId;
import br.com.erpcore.domain.contact.ContactId;
import br.com.erpcore.domain.shared.Money;
import br.com.erpcore.domain.shared.Quantity;
import br.com.erpcore.domain.vehicle.VehicleId;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class QuoteTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final Instant CREATED = Instant.parse("2026-07-01T13:00:00Z");
    /** 01/07/2026 12:00 em São Paulo. */
    private static final Instant SENT_AT = Instant.parse("2026-07-01T15:00:00Z");
    /** Enviado em 01/07 com 7 dias de validade: vale até o fim do dia 08/07, ou seja, vence em 09/07 00:00 local. */
    private static final Instant EXPIRES_AT = Instant.parse("2026-07-09T03:00:00Z");

    private static QuoteItem part(String description, String quantity, String unitPrice) {
        return QuoteItem.of(description, QuoteItemKind.PART, Quantity.of(quantity), Money.of(unitPrice));
    }

    private static Quote draft() {
        return Quote.draft(new QuoteId(UUID.randomUUID()), new CompanyId(UUID.randomUUID()),
                new ContactId(UUID.randomUUID()), new VehicleId(UUID.randomUUID()), 7, "  Barulho ao frear ", CREATED);
    }

    private static Quote draftWithItems() {
        Quote quote = draft();
        quote.reviseItems(List.of(part("Pastilha", "2", "89.90"), QuoteItem.of("Mão de obra", QuoteItemKind.LABOR,
                Quantity.of("1.5"), Money.of("80.00"))));
        return quote;
    }

    private static Quote sent() {
        Quote quote = draftWithItems();
        quote.confirmAndSend(SENT_AT, ZONE);
        return quote;
    }

    // ---------------------------------------------------------------- rascunho e itens

    @Test
    void givenValidData_whenCreatingADraft_thenItStartsEmptyAsDraftWithATrimmedNote() {
        Quote quote = draft();

        assertThat(quote.status()).isEqualTo(QuoteStatus.DRAFT);
        assertThat(quote.items()).isEmpty();
        assertThat(quote.total()).isEqualTo(Money.zero());
        assertThat(quote.note()).isEqualTo("Barulho ao frear");
        assertThat(quote.createdAt()).isEqualTo(CREATED);
        assertThat(quote.sentAt()).isEmpty();
        assertThat(quote.expiresAt()).isEmpty();
    }

    @Test
    void givenNoNote_whenCreatingADraft_thenTheNoteIsEmpty() {
        Quote quote = Quote.draft(new QuoteId(UUID.randomUUID()), new CompanyId(UUID.randomUUID()),
                new ContactId(UUID.randomUUID()), new VehicleId(UUID.randomUUID()), 7, null, CREATED);

        assertThat(quote.note()).isEmpty();
    }

    @Test
    void givenANoteThatIsTooLong_whenCreatingADraft_thenItIsRejected() {
        assertRejectedWith(() -> Quote.draft(new QuoteId(UUID.randomUUID()), new CompanyId(UUID.randomUUID()),
                new ContactId(UUID.randomUUID()), new VehicleId(UUID.randomUUID()), 7,
                "a".repeat(Quote.NOTE_MAX_LENGTH + 1), CREATED), "note", "TOO_LONG");
    }

    @Test
    void givenAValidityOutsideTheRange_whenCreatingADraft_thenItIsOutOfRange() {
        for (int days : new int[] {0, -1, Quote.MAX_VALIDITY_DAYS + 1}) {
            assertRejectedWith(() -> Quote.draft(new QuoteId(UUID.randomUUID()), new CompanyId(UUID.randomUUID()),
                    new ContactId(UUID.randomUUID()), new VehicleId(UUID.randomUUID()), days, null, CREATED), "validityDays", "OUT_OF_RANGE");
        }
        assertThat(Quote.draft(new QuoteId(UUID.randomUUID()), new CompanyId(UUID.randomUUID()),
                new ContactId(UUID.randomUUID()), new VehicleId(UUID.randomUUID()), 1, null, CREATED).validityDays()).isEqualTo(1);
    }

    @Test
    void givenItemsWithBrokenCents_whenRevising_thenTheTotalIsTheSumOfTheRoundedLines() {
        Quote quote = draft();

        quote.reviseItems(List.of(part("A", "2.5", "19.99"), part("B", "3", "33.33"), part("C", "0.5", "0.01")));

        // 49,975 -> 49,98; 99,99; 0,005 -> 0,01
        assertThat(quote.total()).isEqualTo(Money.of("149.98"));
    }

    @Test
    void givenNewItems_whenRevising_thenTheyReplaceTheOldOnesInOrder() {
        Quote quote = draftWithItems();

        quote.reviseItems(List.of(part("Só esta", "1", "10")));

        assertThat(quote.items()).extracting(QuoteItem::description).containsExactly("Só esta");
        assertThat(quote.total()).isEqualTo(Money.of("10.00"));
    }

    @Test
    void givenTheReturnedItemList_whenTryingToChangeIt_thenItIsReadOnly() {
        Quote quote = draftWithItems();

        assertThatThrownBy(() -> quote.items().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void givenTooManyItems_whenRevising_thenItIsOutOfRange() {
        List<QuoteItem> items = new ArrayList<>();
        for (int i = 0; i <= Quote.MAX_ITEMS; i++) {
            items.add(part("Item " + i, "1", "1"));
        }

        assertRejectedWith(() -> draft().reviseItems(items), "items", "OUT_OF_RANGE");
    }

    @Test
    void givenATotalAboveTheLimit_whenRevising_thenItIsOutOfRange() {
        List<QuoteItem> items = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            items.add(part("Item " + i, "1", "1000000.00"));
        }

        assertRejectedWith(() -> draft().reviseItems(items), "total", "OUT_OF_RANGE");
    }

    @Test
    void givenAQuoteThatLeftTheDraftStage_whenRevising_thenTheItemsDoNotChange() {
        Quote quote = sent();

        assertThatThrownBy(() -> quote.reviseItems(List.of(part("Outro", "1", "1"))))
                .isInstanceOfSatisfying(InvalidQuoteTransitionException.class, e -> assertThat(e.from()).isEqualTo(QuoteStatus.SENT));
        assertThat(quote.items()).hasSize(2);
    }

    // ---------------------------------------------------------------- confirmação do dono e envio

    @Test
    void givenADraftWithItems_whenRequestingOwnerConfirmation_thenItWaitsForTheOwner() {
        Quote quote = draftWithItems();

        quote.requestOwnerConfirmation();

        assertThat(quote.status()).isEqualTo(QuoteStatus.AWAITING_OWNER_CONFIRMATION);
    }

    @Test
    void givenADraftWithoutItems_whenRequestingOwnerConfirmationOrSending_thenItIsNotReady() {
        assertRejectedWith(() -> draft().requestOwnerConfirmation(), "items", "REQUIRED");
        assertRejectedWith(() -> draft().confirmAndSend(SENT_AT, ZONE), "items", "REQUIRED");
    }

    @Test
    void givenOnlyAZeroValueItem_whenRequestingOwnerConfirmationOrSending_thenTheTotalMustBePositive() {
        Quote quote = draft();
        quote.reviseItems(List.of(part("Cortesia", "1", "0")));

        assertRejectedWith(quote::requestOwnerConfirmation, "total", "OUT_OF_RANGE");
        assertRejectedWith(() -> quote.confirmAndSend(SENT_AT, ZONE), "total", "OUT_OF_RANGE");
    }

    @Test
    void givenAQuoteAlreadyWaitingForTheOwner_whenRequestingAgain_thenItIsAnInvalidTransition() {
        Quote quote = draftWithItems();
        quote.requestOwnerConfirmation();

        assertThatThrownBy(quote::requestOwnerConfirmation).isInstanceOf(InvalidQuoteTransitionException.class);
    }

    @Test
    void givenADraftOrAQuoteWaitingForTheOwner_whenConfirmingAndSending_thenItIsSentWithTheValidityUntilTheEndOfTheDay() {
        Quote fromDraft = draftWithItems();
        Quote fromAwaiting = draftWithItems();
        fromAwaiting.requestOwnerConfirmation();

        fromDraft.confirmAndSend(SENT_AT, ZONE);
        fromAwaiting.confirmAndSend(SENT_AT, ZONE);

        for (Quote quote : List.of(fromDraft, fromAwaiting)) {
            assertThat(quote.status()).isEqualTo(QuoteStatus.SENT);
            assertThat(quote.sentAt()).hasValue(SENT_AT);
            assertThat(quote.expiresAt()).hasValue(EXPIRES_AT);
        }
    }

    @Test
    void givenAnInstantNearMidnightUtc_whenSending_thenTheDayIsTheLocalDay() {
        Quote quote = draftWithItems();

        // 02/07 02:30 UTC = 01/07 23:30 em São Paulo: o dia local é 01/07, então o prazo é o mesmo do exemplo.
        quote.confirmAndSend(Instant.parse("2026-07-02T02:30:00Z"), ZONE);

        assertThat(quote.expiresAt()).hasValue(EXPIRES_AT);
    }

    @Test
    void givenAQuoteAlreadySent_whenSendingAgain_thenItIsAnInvalidTransition() {
        Quote quote = sent();

        assertThatThrownBy(() -> quote.confirmAndSend(SENT_AT, ZONE)).isInstanceOf(InvalidQuoteTransitionException.class);
    }

    // ---------------------------------------------------------------- decisão pelo cliente

    @Test
    void givenASentQuote_whenApproving_thenItIsApprovedAndNoLongerEditable() {
        Quote quote = sent();
        Instant decidedAt = Instant.parse("2026-07-02T10:00:00Z");

        quote.approve(decidedAt);

        assertThat(quote.status()).isEqualTo(QuoteStatus.APPROVED);
        assertThat(quote.decidedAt()).hasValue(decidedAt);
        assertThatThrownBy(() -> quote.reviseItems(List.of(part("x", "1", "1")))).isInstanceOf(InvalidQuoteTransitionException.class);
    }

    @Test
    void givenAnApprovedQuote_whenApprovingAgain_thenNothingChanges() {
        Quote quote = sent();
        quote.approve(Instant.parse("2026-07-02T10:00:00Z"));

        quote.approve(Instant.parse("2026-07-03T10:00:00Z"));

        assertThat(quote.status()).isEqualTo(QuoteStatus.APPROVED);
        assertThat(quote.decidedAt()).hasValue(Instant.parse("2026-07-02T10:00:00Z"));
    }

    @Test
    void givenASentQuote_whenRejecting_thenItIsRejectedWithATrimmedReason() {
        Quote quote = sent();

        quote.reject(Instant.parse("2026-07-02T10:00:00Z"), "  Achei caro ");

        assertThat(quote.status()).isEqualTo(QuoteStatus.REJECTED);
        assertThat(quote.decisionReason()).hasValue("Achei caro");
    }

    @Test
    void givenNoOrBlankReason_whenRejecting_thenThereIsNoReason() {
        Quote withNull = sent();
        Quote withBlank = sent();

        withNull.reject(Instant.parse("2026-07-02T10:00:00Z"), null);
        withBlank.reject(Instant.parse("2026-07-02T10:00:00Z"), "   ");

        assertThat(withNull.decisionReason()).isEmpty();
        assertThat(withBlank.decisionReason()).isEmpty();
    }

    @Test
    void givenAReasonThatIsTooLong_whenRejecting_thenItIsRejectedAndTheQuoteStaysSent() {
        Quote quote = sent();

        assertRejectedWith(() -> quote.reject(Instant.parse("2026-07-02T10:00:00Z"), "a".repeat(Quote.REASON_MAX_LENGTH + 1)),
                "reason", "TOO_LONG");
        assertThat(quote.status()).isEqualTo(QuoteStatus.SENT);
    }

    @Test
    void givenARejectedQuote_whenRejectingAgain_thenNothingChanges() {
        Quote quote = sent();
        quote.reject(Instant.parse("2026-07-02T10:00:00Z"), "Achei caro");

        quote.reject(Instant.parse("2026-07-03T10:00:00Z"), "Outro motivo");

        assertThat(quote.decisionReason()).hasValue("Achei caro");
        assertThat(quote.decidedAt()).hasValue(Instant.parse("2026-07-02T10:00:00Z"));
    }

    @Test
    void givenADecisionAlreadyTaken_whenTheOppositeDecisionArrives_thenItIsAnInvalidTransition() {
        Quote approved = sent();
        approved.approve(Instant.parse("2026-07-02T10:00:00Z"));
        Quote rejected = sent();
        rejected.reject(Instant.parse("2026-07-02T10:00:00Z"), null);

        assertThatThrownBy(() -> approved.reject(Instant.parse("2026-07-03T10:00:00Z"), null)).isInstanceOf(InvalidQuoteTransitionException.class);
        assertThatThrownBy(() -> rejected.approve(Instant.parse("2026-07-03T10:00:00Z"))).isInstanceOf(InvalidQuoteTransitionException.class);
    }

    @Test
    void givenAQuoteThatWasNeverSent_whenTheClientDecides_thenItIsAnInvalidTransition() {
        assertThatThrownBy(() -> draftWithItems().approve(SENT_AT)).isInstanceOf(InvalidQuoteTransitionException.class);
        assertThatThrownBy(() -> draftWithItems().reject(SENT_AT, null)).isInstanceOf(InvalidQuoteTransitionException.class);
    }

    @Test
    void givenTheMomentOfExpiry_whenDeciding_thenOneSecondBeforeStillWorksAndTheExactInstantDoesNot() {
        Quote beforeLimit = sent();
        Quote atLimit = sent();

        beforeLimit.approve(EXPIRES_AT.minusSeconds(1));

        assertThat(beforeLimit.status()).isEqualTo(QuoteStatus.APPROVED);
        assertThatThrownBy(() -> atLimit.approve(EXPIRES_AT)).isInstanceOf(QuoteUnavailableException.class);
        assertThatThrownBy(() -> atLimit.reject(EXPIRES_AT, null)).isInstanceOf(QuoteUnavailableException.class);
        assertThat(atLimit.status()).isEqualTo(QuoteStatus.SENT);
    }

    // ---------------------------------------------------------------- cancelamento e expiração

    @Test
    void givenAQuoteNotYetDecided_whenCancelling_thenItIsCancelled() {
        Quote fromDraft = draft();
        Quote fromAwaiting = draftWithItems();
        fromAwaiting.requestOwnerConfirmation();
        Quote fromSent = sent();

        for (Quote quote : List.of(fromDraft, fromAwaiting, fromSent)) {
            quote.cancel(Instant.parse("2026-07-02T10:00:00Z"));

            assertThat(quote.status()).isEqualTo(QuoteStatus.CANCELLED);
        }
    }

    @Test
    void givenACancelledQuote_whenCancellingAgain_thenNothingChanges() {
        Quote quote = sent();
        quote.cancel(Instant.parse("2026-07-02T10:00:00Z"));

        quote.cancel(Instant.parse("2026-07-03T10:00:00Z"));

        assertThat(quote.status()).isEqualTo(QuoteStatus.CANCELLED);
        assertThat(quote.decidedAt()).hasValue(Instant.parse("2026-07-02T10:00:00Z"));
    }

    @Test
    void givenADecidedOrExpiredQuote_whenCancelling_thenItIsAnInvalidTransition() {
        Quote approved = sent();
        approved.approve(Instant.parse("2026-07-02T10:00:00Z"));
        Quote rejected = sent();
        rejected.reject(Instant.parse("2026-07-02T10:00:00Z"), null);
        Quote expired = sent();
        expired.expireIfDue(EXPIRES_AT);

        for (Quote quote : List.of(approved, rejected, expired)) {
            assertThatThrownBy(() -> quote.cancel(EXPIRES_AT.plusSeconds(1))).isInstanceOf(InvalidQuoteTransitionException.class);
        }
    }

    @Test
    void givenACancelledQuote_whenTheClientDecides_thenItIsAnInvalidTransition() {
        Quote quote = sent();
        quote.cancel(Instant.parse("2026-07-02T10:00:00Z"));

        assertThatThrownBy(() -> quote.approve(Instant.parse("2026-07-03T10:00:00Z"))).isInstanceOf(InvalidQuoteTransitionException.class);
    }

    @Test
    void givenASentQuote_whenItsTimeRunsOut_thenExpiringMarksItExpiredOnlyAtOrAfterTheLimit() {
        Quote quote = sent();

        assertThat(quote.expireIfDue(EXPIRES_AT.minusSeconds(1))).isFalse();
        assertThat(quote.status()).isEqualTo(QuoteStatus.SENT);
        assertThat(quote.expireIfDue(EXPIRES_AT)).isTrue();
        assertThat(quote.status()).isEqualTo(QuoteStatus.EXPIRED);
        assertThat(quote.expireIfDue(EXPIRES_AT.plusSeconds(60))).isFalse();
    }

    @Test
    void givenAQuoteThatIsNotSent_whenExpiring_thenNothingHappens() {
        Quote quote = draftWithItems();

        assertThat(quote.expireIfDue(EXPIRES_AT.plusSeconds(1))).isFalse();
        assertThat(quote.status()).isEqualTo(QuoteStatus.DRAFT);
    }

    @Test
    void givenASentQuotePastItsLimitBeforeTheJobRuns_whenAskingTheStatus_thenItAlreadyReadsAsExpired() {
        Quote quote = sent();

        assertThat(quote.statusAt(EXPIRES_AT.minusSeconds(1))).isEqualTo(QuoteStatus.SENT);
        assertThat(quote.statusAt(EXPIRES_AT)).isEqualTo(QuoteStatus.EXPIRED);
        assertThat(quote.status()).isEqualTo(QuoteStatus.SENT);
    }

    @Test
    void givenAnExpiredQuote_whenTheClientDecides_thenItIsAnInvalidTransition() {
        Quote quote = sent();
        quote.expireIfDue(EXPIRES_AT);

        assertThatThrownBy(() -> quote.approve(EXPIRES_AT.plusSeconds(1))).isInstanceOf(InvalidQuoteTransitionException.class);
    }

    @Test
    void givenTheStatuses_whenAskingIfTheyAreFinal_thenOnlyTheEndStatesAre() {
        assertThat(QuoteStatus.values()).filteredOn(QuoteStatus::isFinal)
                .containsExactlyInAnyOrder(QuoteStatus.APPROVED, QuoteStatus.REJECTED, QuoteStatus.EXPIRED, QuoteStatus.CANCELLED);
    }
}
