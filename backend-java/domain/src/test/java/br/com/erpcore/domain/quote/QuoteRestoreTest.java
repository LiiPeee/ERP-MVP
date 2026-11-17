package br.com.erpcore.domain.quote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.erpcore.domain.company.CompanyId;
import br.com.erpcore.domain.contact.ContactId;
import br.com.erpcore.domain.identity.TokenHash;
import br.com.erpcore.domain.shared.Money;
import br.com.erpcore.domain.shared.Quantity;
import br.com.erpcore.domain.vehicle.VehicleId;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Reconstituir do armazenamento não aplica regras de transição: devolve o estado exatamente como foi gravado. */
class QuoteRestoreTest {

    private static final Instant CREATED = Instant.parse("2026-07-01T13:00:00Z");
    private static final Instant SENT_AT = Instant.parse("2026-07-01T15:00:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-07-09T03:00:00Z");

    private static final List<QuoteItem> ITEMS = List.of(
            QuoteItem.of("Pastilha", QuoteItemKind.PART, Quantity.of("2"), Money.of("89.90")));

    private static Quote restoredSent() {
        return Quote.restore(new QuoteId(UUID.randomUUID()), new CompanyId(UUID.randomUUID()),
                new ContactId(UUID.randomUUID()), new VehicleId(UUID.randomUUID()), 7, "Observação", CREATED, ITEMS,
                QuoteStatus.SENT, SENT_AT, EXPIRES_AT, null, null);
    }

    @Test
    void givenStoredState_whenRestoring_thenEveryFieldIsExactlyAsStored() {
        Quote quote = Quote.restore(new QuoteId(UUID.randomUUID()), new CompanyId(UUID.randomUUID()),
                new ContactId(UUID.randomUUID()), new VehicleId(UUID.randomUUID()), 7, "Observação", CREATED, ITEMS,
                QuoteStatus.REJECTED, SENT_AT, EXPIRES_AT, Instant.parse("2026-07-02T10:00:00Z"), "Achei caro");

        assertThat(quote.status()).isEqualTo(QuoteStatus.REJECTED);
        assertThat(quote.items()).isEqualTo(ITEMS);
        assertThat(quote.total()).isEqualTo(Money.of("179.80"));
        assertThat(quote.note()).isEqualTo("Observação");
        assertThat(quote.validityDays()).isEqualTo(7);
        assertThat(quote.createdAt()).isEqualTo(CREATED);
        assertThat(quote.sentAt()).hasValue(SENT_AT);
        assertThat(quote.expiresAt()).hasValue(EXPIRES_AT);
        assertThat(quote.decidedAt()).hasValue(Instant.parse("2026-07-02T10:00:00Z"));
        assertThat(quote.decisionReason()).hasValue("Achei caro");
    }

    @Test
    void givenAStoredDraft_whenRestoring_thenTheOptionalMomentsAreAbsent() {
        Quote quote = Quote.restore(new QuoteId(UUID.randomUUID()), new CompanyId(UUID.randomUUID()),
                new ContactId(UUID.randomUUID()), new VehicleId(UUID.randomUUID()), 7, "", CREATED, List.of(),
                QuoteStatus.DRAFT, null, null, null, null);

        assertThat(quote.sentAt()).isEmpty();
        assertThat(quote.expiresAt()).isEmpty();
        assertThat(quote.decidedAt()).isEmpty();
        assertThat(quote.decisionReason()).isEmpty();
    }

    @Test
    void givenARestoredSentQuote_whenTheClientApproves_thenTheRulesStillApply() {
        Quote quote = restoredSent();

        quote.approve(Instant.parse("2026-07-02T10:00:00Z"));

        assertThat(quote.status()).isEqualTo(QuoteStatus.APPROVED);
    }

    @Test
    void givenARestoredSentQuote_whenTheTimeRunsOut_thenItCanStillBeDecidedNoMore() {
        Quote quote = restoredSent();

        assertThatThrownBy(() -> quote.approve(EXPIRES_AT)).isInstanceOf(QuoteUnavailableException.class);
    }

    @Test
    void givenStoredLinkState_whenRestoring_thenItIsExactlyAsStored() {
        QuoteId quoteId = new QuoteId(UUID.randomUUID());
        CompanyId companyId = new CompanyId(UUID.randomUUID());
        TokenHash hash = new TokenHash("b".repeat(64));

        QuoteLink link = QuoteLink.restore(quoteId, companyId, hash, SENT_AT, EXPIRES_AT, Instant.parse("2026-07-02T10:00:00Z"));

        assertThat(link.quoteId()).isEqualTo(quoteId);
        assertThat(link.companyId()).isEqualTo(companyId);
        assertThat(link.tokenHash()).isEqualTo(hash);
        assertThat(link.issuedAt()).isEqualTo(SENT_AT);
        assertThat(link.expiresAt()).isEqualTo(EXPIRES_AT);
        assertThat(link.decidedAt()).hasValue(Instant.parse("2026-07-02T10:00:00Z"));
        assertThat(link.canDecideAt(SENT_AT)).isFalse();
    }

    @Test
    void givenAStoredLinkNeverDecided_whenRestoring_thenItCanStillDecide() {
        QuoteLink link = QuoteLink.restore(new QuoteId(UUID.randomUUID()), new CompanyId(UUID.randomUUID()),
                new TokenHash("c".repeat(64)), SENT_AT, EXPIRES_AT, null);

        assertThat(link.decidedAt()).isEmpty();
        assertThat(link.canDecideAt(SENT_AT)).isTrue();
    }
}
