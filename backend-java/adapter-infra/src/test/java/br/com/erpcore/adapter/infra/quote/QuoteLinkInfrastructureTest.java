package br.com.erpcore.adapter.infra.quote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.erpcore.application.quote.port.out.NewQuoteLinkSecret;
import br.com.erpcore.application.quote.port.out.QuoteDeliveryRequest;
import br.com.erpcore.domain.company.CompanyId;
import br.com.erpcore.domain.contact.ContactId;
import br.com.erpcore.domain.quote.QuoteId;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class QuoteLinkInfrastructureTest {

    private final Sha256QuoteLinkCodec codec = new Sha256QuoteLinkCodec();

    @Test
    void givenANewLinkToken_whenGenerating_thenItHasAtLeast256BitsOfUrlSafeText() {
        assertThat(codec.generate().value()).matches("[A-Za-z0-9_-]{43,}");
    }

    @Test
    void givenManyTokens_whenGenerating_thenNoneRepeats() {
        Set<String> seen = new HashSet<>();
        for (int index = 0; index < 1000; index++) {
            seen.add(codec.generate().value());
        }

        assertThat(seen).hasSize(1000);
    }

    @Test
    void givenANewToken_whenGenerating_thenItsHashIsTheHashOfItsValueAndNeverTheValueItself() {
        NewQuoteLinkSecret secret = codec.generate();

        assertThat(secret.hash()).isEqualTo(codec.hash(secret.value())).matches("[0-9a-f]{64}").isNotEqualTo(secret.value());
    }

    @Test
    void givenAKnownInput_whenHashing_thenItMatchesTheSha256TestVector() {
        assertThat(codec.hash("abc")).isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    void givenABaseAddress_whenBuildingTheLink_thenTheTokenGoesAfterTheLinkPath() {
        assertThat(new ConfiguredPublicQuoteUrls("https://oficina.exemplo.test").forToken("abc")).isEqualTo("https://oficina.exemplo.test/o/abc");
        assertThat(new ConfiguredPublicQuoteUrls("https://oficina.exemplo.test/").forToken("abc")).isEqualTo("https://oficina.exemplo.test/o/abc");
    }

    @Test
    void givenAnInvalidBaseAddress_whenCreating_thenItIsRefused() {
        assertThatThrownBy(() -> new ConfiguredPublicQuoteUrls("")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ConfiguredPublicQuoteUrls("ftp://x.test")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ConfiguredPublicQuoteUrls("localhost:8080")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenNoAutomaticChannelYet_whenDelivering_thenItFailsSoTheOwnerKnowsToShareTheLinkHimself() {
        QuoteDeliveryRequest request = new QuoteDeliveryRequest(new CompanyId(UUID.randomUUID()), new QuoteId(UUID.randomUUID()),
                new ContactId(UUID.randomUUID()), "https://oficina.exemplo.test/o/abc");

        assertThatThrownBy(() -> new ManualQuoteDelivery().deliver(request)).isInstanceOf(IllegalStateException.class);
    }
}
