package br.com.erpcore.application.contract;

import static br.com.erpcore.application.contract.BusinessContractData.SENT_AT;
import static br.com.erpcore.application.contract.BusinessContractData.contact;
import static br.com.erpcore.application.contract.BusinessContractData.draftQuote;
import static br.com.erpcore.application.contract.BusinessContractData.quoteWith;
import static br.com.erpcore.application.contract.BusinessContractData.vehicle;
import static br.com.erpcore.application.contract.ContractData.company;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.erpcore.application.quote.port.out.QuoteRepository;
import br.com.erpcore.domain.company.Company;
import br.com.erpcore.domain.contact.Contact;
import br.com.erpcore.domain.quote.Quote;
import br.com.erpcore.domain.quote.QuoteId;
import br.com.erpcore.domain.quote.QuoteItem;
import br.com.erpcore.domain.quote.QuoteStatus;
import br.com.erpcore.domain.vehicle.Vehicle;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** O mesmo contrato roda contra o fake em memória e contra o adaptador de PostgreSQL. */
public abstract class QuoteRepositoryContract extends BusinessContract {

    private static final Instant FUTURE = SENT_AT.plusSeconds(7 * 24 * 3600L);

    private final Company company = company(1);
    private final Company otherCompany = company(2);
    private final Contact owner = contact(1, company);
    private final Contact otherOwner = contact(2, otherCompany);
    private final Vehicle car = vehicle(1, company, owner);
    private final Vehicle otherCar = vehicle(2, otherCompany, otherOwner);

    protected abstract QuoteRepository repository();

    private void prepare() {
        givenCompaniesExist(company, otherCompany);
        givenContactsExist(owner, otherOwner);
        givenVehiclesExist(car, otherCar);
    }

    private void save(Quote quote) {
        inTransaction(() -> repository().save(quote));
    }

    @Test
    void givenASavedDraft_whenFindingItById_thenEveryFieldAndTheItemsInOrderComeBack() {
        prepare();
        Quote saved = draftQuote(1, company, owner, car);
        save(saved);

        Quote found = inTransaction(() -> repository().findById(company.id(), saved.id())).orElseThrow();

        assertThat(found.id()).isEqualTo(saved.id());
        assertThat(found.companyId()).isEqualTo(company.id());
        assertThat(found.contactId()).isEqualTo(owner.id());
        assertThat(found.vehicleId()).isEqualTo(car.id());
        assertThat(found.status()).isEqualTo(QuoteStatus.DRAFT);
        assertThat(found.validityDays()).isEqualTo(7);
        assertThat(found.note()).isEqualTo(saved.note());
        assertThat(found.createdAt()).isEqualTo(saved.createdAt());
        assertThat(found.items()).isEqualTo(saved.items());
        assertThat(found.total()).isEqualTo(saved.total());
        assertThat(found.sentAt()).isEmpty();
        assertThat(found.expiresAt()).isEmpty();
        assertThat(found.decidedAt()).isEmpty();
        assertThat(found.decisionReason()).isEmpty();
    }

    @Test
    void givenADecidedQuote_whenSavingItAgain_thenTheNewStateIsStoredAndTheItemsAreKeptOnce() {
        prepare();
        Quote quote = draftQuote(1, company, owner, car);
        save(quote);
        Quote sent = inTransaction(() -> repository().findById(company.id(), quote.id())).orElseThrow();
        sent.confirmAndSend(SENT_AT, java.time.ZoneId.of("America/Sao_Paulo"));
        save(sent);
        Quote loaded = inTransaction(() -> repository().findById(company.id(), quote.id())).orElseThrow();
        loaded.reject(SENT_AT.plusSeconds(3600), "Achei caro");

        save(loaded);

        Quote found = inTransaction(() -> repository().findById(company.id(), quote.id())).orElseThrow();
        assertThat(found.status()).isEqualTo(QuoteStatus.REJECTED);
        assertThat(found.sentAt()).hasValue(SENT_AT);
        assertThat(found.expiresAt()).hasValue(Instant.parse("2026-07-09T03:00:00Z"));
        assertThat(found.decidedAt()).hasValue(SENT_AT.plusSeconds(3600));
        assertThat(found.decisionReason()).hasValue("Achei caro");
        assertThat(found.items()).hasSize(2);
    }

    @Test
    void givenADraftWhoseItemsChange_whenSavingItAgain_thenTheNewItemsReplaceTheOldOnes() {
        prepare();
        Quote quote = draftQuote(1, company, owner, car);
        save(quote);
        Quote loaded = inTransaction(() -> repository().findById(company.id(), quote.id())).orElseThrow();
        List<QuoteItem> onlyOne = List.of(loaded.items().get(1));
        loaded.reviseItems(onlyOne);

        save(loaded);

        assertThat(inTransaction(() -> repository().findById(company.id(), quote.id())).orElseThrow().items()).isEqualTo(onlyOne);
    }

    @Test
    void givenNoSuchQuote_whenFindingIt_thenTheResultIsEmpty() {
        prepare();

        assertThat(inTransaction(() -> repository().findById(company.id(), new QuoteId(UUID.randomUUID())))).isEmpty();
        assertThat(inTransaction(() -> repository().findByIdForUpdate(company.id(), new QuoteId(UUID.randomUUID())))).isEmpty();
    }

    @Test
    void givenAQuoteOfAnotherCompany_whenFindingIt_thenItIsNotFound() {
        prepare();
        Quote saved = draftQuote(1, company, owner, car);
        save(saved);

        assertThat(inTransaction(() -> repository().findById(otherCompany.id(), saved.id()))).isEmpty();
        assertThat(inTransaction(() -> repository().findByIdForUpdate(otherCompany.id(), saved.id()))).isEmpty();
    }

    @Test
    void givenASavedQuote_whenFindingItForUpdate_thenItIsTheSameQuote() {
        prepare();
        Quote saved = draftQuote(1, company, owner, car);
        save(saved);

        assertThat(inTransaction(() -> repository().findByIdForUpdate(company.id(), saved.id())).orElseThrow().id()).isEqualTo(saved.id());
    }

    @Test
    void givenQuotesInSeveralStatuses_whenListingByStatus_thenOnlyThoseOfTheCompanyComeBackNewestFirstUpToTheLimit() {
        prepare();
        save(quoteWith(1, company, owner, car, QuoteStatus.SENT, FUTURE));
        save(quoteWith(2, company, owner, car, QuoteStatus.APPROVED, FUTURE));
        save(quoteWith(3, company, owner, car, QuoteStatus.SENT, FUTURE));
        save(quoteWith(4, company, owner, car, QuoteStatus.SENT, FUTURE));
        save(quoteWith(5, otherCompany, otherOwner, otherCar, QuoteStatus.SENT, FUTURE));

        List<Quote> all = inTransaction(() -> repository().findByStatuses(company.id(), Set.of(QuoteStatus.SENT), 10));
        List<Quote> limited = inTransaction(() -> repository().findByStatuses(company.id(), Set.of(QuoteStatus.SENT), 2));
        List<Quote> several = inTransaction(() -> repository().findByStatuses(company.id(), Set.of(QuoteStatus.SENT, QuoteStatus.APPROVED), 10));

        assertThat(all).extracting(quote -> quote.id().value()).containsExactly(ContractData.uuid(4), ContractData.uuid(3), ContractData.uuid(1));
        assertThat(limited).extracting(quote -> quote.id().value()).containsExactly(ContractData.uuid(4), ContractData.uuid(3));
        assertThat(several).hasSize(4);
    }

    @Test
    void givenAnEmptySetOfStatuses_whenListing_thenTheListIsEmpty() {
        prepare();
        save(quoteWith(1, company, owner, car, QuoteStatus.SENT, FUTURE));

        assertThat(inTransaction(() -> repository().findByStatuses(company.id(), Set.of(), 10))).isEmpty();
    }

    @Test
    void givenSentQuotesOfSeveralCompanies_whenLookingForThoseDueToExpire_thenOnlyThoseAtOrPastTheLimitComeBack() {
        prepare();
        Instant now = Instant.parse("2026-07-09T03:00:00Z");
        save(quoteWith(1, company, owner, car, QuoteStatus.SENT, now.minusSeconds(1)));
        save(quoteWith(2, otherCompany, otherOwner, otherCar, QuoteStatus.SENT, now));
        save(quoteWith(3, company, owner, car, QuoteStatus.SENT, now.plusSeconds(1)));
        save(quoteWith(4, company, owner, car, QuoteStatus.APPROVED, now.minusSeconds(100)));
        save(draftQuote(5, company, owner, car));

        List<Quote> due = inTransaction(() -> repository().findDueForExpiry(now, 10));

        assertThat(due).extracting(quote -> quote.id().value()).containsExactlyInAnyOrder(ContractData.uuid(1), ContractData.uuid(2));
        assertThat(inTransaction(() -> repository().findDueForExpiry(now, 1))).hasSize(1);
    }
}
