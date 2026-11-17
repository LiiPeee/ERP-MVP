package br.com.erpcore.domain.quote;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.erpcore.domain.company.CompanyId;
import br.com.erpcore.domain.identity.TokenHash;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class QuoteLinkTest {

    private static final Instant ISSUED = Instant.parse("2026-07-01T15:00:00Z");
    private static final Instant EXPIRES = Instant.parse("2026-07-09T03:00:00Z");

    private static QuoteLink link() {
        return QuoteLink.issue(new QuoteId(UUID.randomUUID()), new CompanyId(UUID.randomUUID()),
                new TokenHash("a".repeat(64)), ISSUED, EXPIRES);
    }

    @Test
    void givenANewLink_whenAskedAtDifferentMoments_thenItIsActiveOnlyBeforeItsExpiry() {
        QuoteLink link = link();

        assertThat(link.isActiveAt(ISSUED)).isTrue();
        assertThat(link.isActiveAt(EXPIRES.minusSeconds(1))).isTrue();
        assertThat(link.isActiveAt(EXPIRES)).isFalse();
    }

    @Test
    void givenANewLink_whenAskingIfItCanDecide_thenItCanUntilADecisionIsRecorded() {
        QuoteLink link = link();

        assertThat(link.canDecideAt(ISSUED)).isTrue();
        link.markDecided(Instant.parse("2026-07-02T10:00:00Z"));

        assertThat(link.canDecideAt(ISSUED)).isFalse();
        assertThat(link.isActiveAt(ISSUED)).isTrue();
        assertThat(link.decidedAt()).hasValue(Instant.parse("2026-07-02T10:00:00Z"));
    }

    @Test
    void givenAnExpiredLink_whenAskingIfItCanDecide_thenItCannot() {
        assertThat(link().canDecideAt(EXPIRES)).isFalse();
    }

    @Test
    void givenADecidedLink_whenMarkingAgain_thenTheFirstDecisionTimeIsKept() {
        QuoteLink link = link();
        link.markDecided(Instant.parse("2026-07-02T10:00:00Z"));

        link.markDecided(Instant.parse("2026-07-03T10:00:00Z"));

        assertThat(link.decidedAt()).hasValue(Instant.parse("2026-07-02T10:00:00Z"));
    }

    @Test
    void givenALink_whenPrinting_thenTheHashNeverAppears() {
        assertThat(link().toString()).doesNotContain("aaaa");
    }
}
