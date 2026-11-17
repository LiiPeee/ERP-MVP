package br.com.erpcore.application.contract;

import static br.com.erpcore.application.contract.BusinessContractData.SENT_AT;
import static br.com.erpcore.application.contract.BusinessContractData.contact;
import static br.com.erpcore.application.contract.BusinessContractData.draftQuote;
import static br.com.erpcore.application.contract.BusinessContractData.link;
import static br.com.erpcore.application.contract.BusinessContractData.vehicle;
import static br.com.erpcore.application.contract.ContractData.company;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.erpcore.application.quote.port.out.QuoteLinkRepository;
import br.com.erpcore.domain.company.Company;
import br.com.erpcore.domain.contact.Contact;
import br.com.erpcore.domain.identity.TokenHash;
import br.com.erpcore.domain.quote.Quote;
import br.com.erpcore.domain.quote.QuoteLink;
import br.com.erpcore.domain.vehicle.Vehicle;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** O mesmo contrato roda contra o fake em memória e contra o adaptador de PostgreSQL. */
public abstract class QuoteLinkRepositoryContract extends BusinessContract {

    private final Company company = company(1);
    private final Contact owner = contact(1, company);
    private final Vehicle car = vehicle(1, company, owner);
    private final Quote quote = draftQuote(1, company, owner, car);

    protected abstract QuoteLinkRepository repository();

    private void prepare() {
        givenCompaniesExist(company);
        givenContactsExist(owner);
        givenVehiclesExist(car);
        givenQuotesExist(quote);
    }

    @Test
    void givenASavedLink_whenFindingItByHash_thenEveryFieldComesBack() {
        prepare();
        QuoteLink saved = link(1, quote);
        inTransaction(() -> repository().save(saved));

        QuoteLink found = inTransaction(() -> repository().findByHash(saved.tokenHash())).orElseThrow();

        assertThat(found.quoteId()).isEqualTo(quote.id());
        assertThat(found.companyId()).isEqualTo(company.id());
        assertThat(found.tokenHash()).isEqualTo(saved.tokenHash());
        assertThat(found.issuedAt()).isEqualTo(SENT_AT);
        assertThat(found.expiresAt()).isEqualTo(saved.expiresAt());
        assertThat(found.decidedAt()).isEmpty();
    }

    @Test
    void givenALinkThatWasDecided_whenSavingItAgain_thenTheDecisionMomentIsStored() {
        prepare();
        QuoteLink saved = link(1, quote);
        inTransaction(() -> repository().save(saved));
        QuoteLink loaded = inTransaction(() -> repository().findByHash(saved.tokenHash())).orElseThrow();
        loaded.markDecided(Instant.parse("2026-07-02T10:00:00Z"));

        inTransaction(() -> repository().save(loaded));

        assertThat(inTransaction(() -> repository().findByHash(saved.tokenHash())).orElseThrow().decidedAt())
                .hasValue(Instant.parse("2026-07-02T10:00:00Z"));
    }

    @Test
    void givenTwoLinksOfTheSameQuote_whenFindingEachByItsHash_thenEachComesBackWithItsOwnData() {
        prepare();
        inTransaction(() -> repository().save(link(1, quote)));
        inTransaction(() -> repository().save(link(2, quote)));

        assertThat(inTransaction(() -> repository().findByHash(link(1, quote).tokenHash()))).isPresent();
        assertThat(inTransaction(() -> repository().findByHash(link(2, quote).tokenHash()))).isPresent();
    }

    @Test
    void givenAnUnknownHash_whenFindingIt_thenTheResultIsEmpty() {
        prepare();

        assertThat(inTransaction(() -> repository().findByHash(new TokenHash("f".repeat(64))))).isEmpty();
    }
}
