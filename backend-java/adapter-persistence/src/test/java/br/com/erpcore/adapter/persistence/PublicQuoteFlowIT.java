package br.com.erpcore.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.erpcore.adapter.persistence.support.ApplicationRolePostgresTest;
import br.com.erpcore.adapter.persistence.support.PostgresTestDatabase;
import br.com.erpcore.application.contact.ContactView;
import br.com.erpcore.application.contact.CreateContactCommand;
import br.com.erpcore.application.contact.CreateContactUseCase;
import br.com.erpcore.application.identity.AuthenticatedPrincipal;
import br.com.erpcore.application.identity.RegisterCompanyAndOwnerCommand;
import br.com.erpcore.application.identity.RegisterCompanyAndOwnerResult;
import br.com.erpcore.application.identity.RegisterCompanyAndOwnerUseCase;
import br.com.erpcore.application.quote.ApproveQuoteByLinkUseCase;
import br.com.erpcore.application.quote.ConfirmQuoteCommand;
import br.com.erpcore.application.quote.ConfirmQuoteUseCase;
import br.com.erpcore.application.quote.CreateQuoteDraftCommand;
import br.com.erpcore.application.quote.CreateQuoteDraftUseCase;
import br.com.erpcore.application.quote.DecideQuoteByLinkCommand;
import br.com.erpcore.application.quote.ExpireDueQuotesUseCase;
import br.com.erpcore.application.quote.GetPublicQuoteQuery;
import br.com.erpcore.application.quote.GetPublicQuoteUseCase;
import br.com.erpcore.application.quote.GetQuoteQuery;
import br.com.erpcore.application.quote.GetQuoteUseCase;
import br.com.erpcore.application.quote.ListQuotesQuery;
import br.com.erpcore.application.quote.ListQuotesUseCase;
import br.com.erpcore.application.quote.PublicQuoteView;
import br.com.erpcore.application.quote.QuoteItemInput;
import br.com.erpcore.application.quote.QuoteSentResult;
import br.com.erpcore.application.quote.RejectQuoteByLinkUseCase;
import br.com.erpcore.application.quote.ReviseQuoteItemsCommand;
import br.com.erpcore.application.quote.ReviseQuoteItemsUseCase;
import br.com.erpcore.application.support.FakePublicQuoteUrls;
import br.com.erpcore.application.vehicle.RegisterVehicleCommand;
import br.com.erpcore.application.vehicle.RegisterVehicleUseCase;
import br.com.erpcore.domain.quote.QuoteAlreadyDecidedException;
import br.com.erpcore.domain.quote.QuoteLinkUnavailableException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

/** O fluxo do orçamento de ponta a ponta sobre o PostgreSQL real, com o papel da aplicação e a RLS ligada. */
@ApplicationRolePostgresTest
@TestPropertySource(properties = "spring.datasource.hikari.maximum-pool-size=6")
class PublicQuoteFlowIT {

    private static final String CORRELATION = "correlacao-fluxo";
    private static final List<QuoteItemInput> ITEMS = List.of(new QuoteItemInput("Pastilha", "PART", "2", "89.90"),
            new QuoteItemInput("Mão de obra", "LABOR", "1.5", "80.00"));

    @Autowired
    private RegisterCompanyAndOwnerUseCase register;
    @Autowired
    private CreateContactUseCase createContact;
    @Autowired
    private RegisterVehicleUseCase registerVehicle;
    @Autowired
    private CreateQuoteDraftUseCase createDraft;
    @Autowired
    private ReviseQuoteItemsUseCase revise;
    @Autowired
    private ConfirmQuoteUseCase confirm;
    @Autowired
    private GetQuoteUseCase getQuote;
    @Autowired
    private ListQuotesUseCase listQuotes;
    @Autowired
    private GetPublicQuoteUseCase getPublic;
    @Autowired
    private ApproveQuoteByLinkUseCase approve;
    @Autowired
    private RejectQuoteByLinkUseCase reject;
    @Autowired
    private ExpireDueQuotesUseCase expire;

    private AuthenticatedPrincipal principal;
    private UUID contactId;
    private UUID vehicleId;

    @BeforeEach
    void aShopWithAClientAndAVehicle() {
        String unique = UUID.randomUUID().toString();
        RegisterCompanyAndOwnerResult owner = register.execute(new RegisterCompanyAndOwnerCommand("Oficina Fictícia " + unique,
                "Dono Fictício", unique + "@exemplo.test", "senha-sintetica-12345", CORRELATION));
        principal = new AuthenticatedPrincipal(owner.userId(), owner.companyId());
        ContactView contact = createContact.execute(new CreateContactCommand(principal, "Cliente Fictício", "11987654321", null, CORRELATION));
        contactId = contact.id();
        vehicleId = registerVehicle.execute(new RegisterVehicleCommand(principal, contactId, "ABC1234", "Fiat", "Uno", 2012, null, CORRELATION)).id();
    }

    private QuoteSentResult sendNew() {
        UUID id = createDraft.execute(new CreateQuoteDraftCommand(principal, contactId, vehicleId, ITEMS, "Barulho ao frear", null, CORRELATION)).id();
        return confirm.execute(new ConfirmQuoteCommand(principal, id, CORRELATION));
    }

    private static String tokenOf(QuoteSentResult sent) {
        return FakePublicQuoteUrls.tokenOf(sent.publicUrl());
    }

    private String statusOf(UUID quoteId) {
        return getQuote.execute(new GetQuoteQuery(principal, quoteId)).status();
    }

    private long auditCount(String type, UUID quoteId) {
        return PostgresTestDatabase.superuserJdbc().sql("SELECT count(*) FROM audit_event WHERE event_type = ? AND details ->> 'quoteId' = ?")
                .param(type).param(quoteId.toString()).query(Long.class).single();
    }

    @Test
    void givenASentQuote_whenTheClientOpensAndApproves_thenThePageShowsTheMinimumAndTheDecisionIsStoredAndAudited() {
        QuoteSentResult sent = sendNew();

        PublicQuoteView view = getPublic.execute(new GetPublicQuoteQuery(tokenOf(sent)));
        String decision = approve.execute(new DecideQuoteByLinkCommand(tokenOf(sent), null, CORRELATION)).status();

        assertThat(view.shopName()).startsWith("Oficina Fictícia");
        assertThat(view.vehicleLabel()).isEqualTo("Fiat Uno 2012 · placa final 234");
        assertThat(view.total()).isEqualTo("299.80");
        assertThat(view.status()).isEqualTo("SENT");
        assertThat(decision).isEqualTo("APPROVED");
        assertThat(statusOf(sent.quoteId())).isEqualTo("APPROVED");
        assertThat(auditCount("QUOTE_APPROVED_BY_LINK", sent.quoteId())).isEqualTo(1);
        assertThat(PostgresTestDatabase.superuserJdbc().sql("SELECT actor_user_id IS NULL FROM audit_event WHERE event_type = 'QUOTE_APPROVED_BY_LINK' AND details ->> 'quoteId' = ?")
                .param(sent.quoteId().toString()).query(Boolean.class).single()).isTrue();
    }

    @Test
    void givenADecision_whenTheClientRepeatsOrContradictsIt_thenRepeatingChangesNothingAndContradictingIsRefused() {
        QuoteSentResult sent = sendNew();
        approve.execute(new DecideQuoteByLinkCommand(tokenOf(sent), null, CORRELATION));

        approve.execute(new DecideQuoteByLinkCommand(tokenOf(sent), null, CORRELATION));

        assertThat(auditCount("QUOTE_APPROVED_BY_LINK", sent.quoteId())).isEqualTo(1);
        assertThatThrownBy(() -> reject.execute(new DecideQuoteByLinkCommand(tokenOf(sent), "mudei de ideia", CORRELATION)))
                .isInstanceOf(QuoteAlreadyDecidedException.class);
        assertThat(statusOf(sent.quoteId())).isEqualTo("APPROVED");
    }

    @Test
    void givenAnUnknownToken_whenOpeningOrDeciding_thenItIsTheSameUnavailableAnswer() {
        assertThatThrownBy(() -> getPublic.execute(new GetPublicQuoteQuery("token-que-nao-existe"))).isExactlyInstanceOf(QuoteLinkUnavailableException.class);
        assertThatThrownBy(() -> approve.execute(new DecideQuoteByLinkCommand("token-que-nao-existe", null, CORRELATION)))
                .isExactlyInstanceOf(QuoteLinkUnavailableException.class);
    }

    @Test
    void givenADeadlineThatPassed_whenTheClientOpensThenTheJobRuns_thenTheLinkIsUnavailableAndTheQuoteIsStoredAsExpired() {
        QuoteSentResult sent = sendNew();
        PostgresTestDatabase.superuserJdbc().sql("UPDATE quote_public_link SET expires_at = now() - interval '1 hour' WHERE quote_id = ?").param(sent.quoteId()).update();
        PostgresTestDatabase.superuserJdbc().sql("UPDATE quote SET expires_at = now() - interval '1 hour' WHERE id = ?").param(sent.quoteId()).update();

        assertThatThrownBy(() -> getPublic.execute(new GetPublicQuoteQuery(tokenOf(sent)))).isExactlyInstanceOf(QuoteLinkUnavailableException.class);
        assertThat(statusOf(sent.quoteId())).isEqualTo("EXPIRED");
        int expired = expire.execute(CORRELATION);

        assertThat(expired).isGreaterThanOrEqualTo(1);
        assertThat(PostgresTestDatabase.superuserJdbc().sql("SELECT status FROM quote WHERE id = ?").param(sent.quoteId()).query(String.class).single()).isEqualTo("EXPIRED");
        assertThat(auditCount("QUOTE_EXPIRED", sent.quoteId())).isEqualTo(1);
        assertThat(listQuotes.execute(new ListQuotesQuery(principal)).closed()).extracting(summary -> summary.id()).contains(sent.quoteId());
    }

    @Test
    void givenRevisedItems_whenReadingThemBack_thenTheCurrentRevisionIsUsedAndTheOldOneStaysAsHistory() {
        UUID id = createDraft.execute(new CreateQuoteDraftCommand(principal, contactId, vehicleId, ITEMS, null, null, CORRELATION)).id();

        var revised = revise.execute(new ReviseQuoteItemsCommand(principal, id, List.of(new QuoteItemInput("Só esta", "PART", "1", "10")), CORRELATION));

        assertThat(revised.total()).isEqualTo("10.00");
        assertThat(getQuote.execute(new GetQuoteQuery(principal, id)).items()).hasSize(1);
        assertThat(PostgresTestDatabase.superuserJdbc().sql("SELECT count(*) FROM quote_item WHERE quote_id = ?").param(id).query(Long.class).single()).isEqualTo(3);
        assertThat(PostgresTestDatabase.superuserJdbc().sql("SELECT items_revision FROM quote WHERE id = ?").param(id).query(Integer.class).single()).isEqualTo(2);
    }

    @Test
    void givenTwoClientsDecidingAtTheSameTime_whenOneApprovesAndTheOtherRejects_thenExactlyOneWinsAndTheStoredStateMatchesIt() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            for (int round = 0; round < 15; round++) {
                QuoteSentResult sent = sendNew();
                CountDownLatch start = new CountDownLatch(1);
                Future<String> approving = pool.submit(() -> decide(start, () -> approve.execute(new DecideQuoteByLinkCommand(tokenOf(sent), null, CORRELATION)).status()));
                Future<String> rejecting = pool.submit(() -> decide(start, () -> reject.execute(new DecideQuoteByLinkCommand(tokenOf(sent), "não quero", CORRELATION)).status()));
                start.countDown();
                String approveResult = approving.get();
                String rejectResult = rejecting.get();

                String stored = statusOf(sent.quoteId());
                assertThat(List.of(approveResult, rejectResult)).as("rodada %d", round).containsAnyOf("APPROVED", "REJECTED");
                assertThat(stored).isIn("APPROVED", "REJECTED");
                long winners = List.of(approveResult, rejectResult).stream().filter(result -> result.equals(stored)).count();
                assertThat(winners).as("quem venceu (%s) na rodada %d", stored, round).isEqualTo(1);
                assertThat(List.of(approveResult, rejectResult)).contains("ALREADY_DECIDED");
                assertThat(auditCount("QUOTE_APPROVED_BY_LINK", sent.quoteId()) + auditCount("QUOTE_REJECTED_BY_LINK", sent.quoteId())).isEqualTo(1);
            }
        } finally {
            pool.shutdownNow();
        }
    }

    private static String decide(CountDownLatch start, java.util.function.Supplier<String> action) throws InterruptedException {
        start.await();
        try {
            return action.get();
        } catch (QuoteAlreadyDecidedException lost) {
            return "ALREADY_DECIDED";
        }
    }
}
