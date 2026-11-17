package br.com.erpcore.adapter.persistence.security;

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
import br.com.erpcore.application.profile.UpdateCompanyProfileCommand;
import br.com.erpcore.application.profile.UpdateCompanyProfileUseCase;
import br.com.erpcore.application.quote.ConfirmQuoteCommand;
import br.com.erpcore.application.quote.ConfirmQuoteUseCase;
import br.com.erpcore.application.quote.CreateQuoteDraftCommand;
import br.com.erpcore.application.quote.CreateQuoteDraftUseCase;
import br.com.erpcore.application.quote.QuoteItemInput;
import br.com.erpcore.application.quote.QuoteSentResult;
import br.com.erpcore.application.shared.port.out.AccessScope;
import br.com.erpcore.application.shared.port.out.UnitOfWork;
import br.com.erpcore.application.support.FakePublicQuoteUrls;
import br.com.erpcore.application.support.FakeQuoteLinkCodec;
import br.com.erpcore.application.vehicle.RegisterVehicleCommand;
import br.com.erpcore.application.vehicle.RegisterVehicleUseCase;
import br.com.erpcore.application.vehicle.VehicleView;
import br.com.erpcore.domain.company.CompanyId;
import br.com.erpcore.domain.identity.Email;
import br.com.erpcore.domain.identity.TokenHash;
import br.com.erpcore.domain.identity.UserId;
import br.com.erpcore.domain.quote.QuoteId;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.TestPropertySource;

/**
 * Duas oficinas no mesmo banco, cada uma com cliente, veículo, orçamento em rascunho, orçamento enviado e link.
 * Prova, com o papel real da aplicação, que o banco isola contato, veículo, orçamento, itens e links por oficina, e
 * que os escopos públicos (link, decisão, expiração) enxergam e alteram o mínimo. Antes de cada tentativa de
 * vazamento, o dado alheio é conferido como existente (superusuário).
 */
@ApplicationRolePostgresTest
@TestPropertySource(properties = "spring.datasource.hikari.maximum-pool-size=1")
class QuoteRowLevelSecurityIT {

    private static final String PASSWORD = "senha-sintetica-12345";
    private static final List<String> TENANT_TABLES = List.of("contact", "vehicle", "quote", "quote_item", "quote_public_link");

    @Autowired
    private UnitOfWork unitOfWork;
    @Autowired
    private JdbcClient jdbc;
    @Autowired
    private RegisterCompanyAndOwnerUseCase registerUseCase;
    @Autowired
    private UpdateCompanyProfileUseCase updateProfile;
    @Autowired
    private CreateContactUseCase createContact;
    @Autowired
    private RegisterVehicleUseCase registerVehicle;
    @Autowired
    private CreateQuoteDraftUseCase createDraft;
    @Autowired
    private ConfirmQuoteUseCase confirmQuote;
    @Autowired
    private FakeQuoteLinkCodec linkCodec;

    private Shop ana;
    private Shop beto;

    private record Shop(RegisterCompanyAndOwnerResult owner, AuthenticatedPrincipal principal, UUID contactId,
                        UUID vehicleId, UUID draftId, UUID sentId, String token) {

        CompanyId company() {
            return new CompanyId(owner.companyId());
        }

        UserId user() {
            return new UserId(owner.userId());
        }

        AccessScope.Tenant tenant() {
            return new AccessScope.Tenant(user(), company());
        }
    }

    @BeforeEach
    void twoShopsEachWithAClientAVehicleADraftAndASentQuote() {
        ana = shop("ana", "11987654321", "ABC1234");
        beto = shop("beto", "11987654322", "XYZ9A87");
    }

    private Shop shop(String name, String phone, String plate) {
        String unique = name + "-" + UUID.randomUUID();
        RegisterCompanyAndOwnerResult owner = registerUseCase.execute(new RegisterCompanyAndOwnerCommand("Oficina Fictícia " + unique,
                "Dono Fictício " + name, unique + "@exemplo.test", PASSWORD, "correlacao-" + name));
        AuthenticatedPrincipal principal = new AuthenticatedPrincipal(owner.userId(), owner.companyId());
        updateProfile.execute(new UpdateCompanyProfileCommand(principal, "Oficina do " + name, "11933334444", null, null, 7, null, "c"));
        ContactView contact = createContact.execute(new CreateContactCommand(principal, "Cliente de " + name, uniquePhone(phone), name + "@cliente.test", "c"));
        VehicleView vehicle = registerVehicle.execute(new RegisterVehicleCommand(principal, contact.id(), plate, "Fiat", "Uno", 2012, null, "c"));
        List<QuoteItemInput> items = List.of(new QuoteItemInput("Pastilha", "PART", "2", "89.90"));
        UUID draft = createDraft.execute(new CreateQuoteDraftCommand(principal, contact.id(), vehicle.id(), items, null, null, "c")).id();
        UUID toSend = createDraft.execute(new CreateQuoteDraftCommand(principal, contact.id(), vehicle.id(), items, null, null, "c")).id();
        QuoteSentResult sent = confirmQuote.execute(new ConfirmQuoteCommand(principal, toSend, "c"));
        return new Shop(owner, principal, contact.id(), vehicle.id(), draft, sent.quoteId(), FakePublicQuoteUrls.tokenOf(sent.publicUrl()));
    }

    /** Cada execução cria dados novos no mesmo banco: o telefone precisa ser único por oficina, e cada oficina é nova. */
    private static String uniquePhone(String base) {
        return base;
    }

    private <T> T as(AccessScope scope, Supplier<T> work) {
        return unitOfWork.execute(scope, work);
    }

    private long count(AccessScope scope, String sql, Object... params) {
        return as(scope, () -> {
            JdbcClient.StatementSpec spec = jdbc.sql(sql);
            for (int i = 0; i < params.length; i++) {
                spec = spec.param(params[i]);
            }
            return spec.query(Long.class).single();
        });
    }

    private long countAsSuperuser(String sql, Object... params) {
        JdbcClient.StatementSpec spec = PostgresTestDatabase.superuserJdbc().sql(sql);
        for (Object param : params) {
            spec = spec.param(param);
        }
        return spec.query(Long.class).single();
    }

    private AccessScope.QuoteDecision decisionFor(Shop shop, UUID quoteId) {
        return new AccessScope.QuoteDecision(shop.company(), new QuoteId(quoteId));
    }

    private TokenHash hashOf(Shop shop) {
        return new TokenHash(linkCodec.hash(shop.token()));
    }

    // ---------------------------------------------------------------- isolamento entre oficinas

    @Test
    void givenAnotherShopsRows_whenReadingThemByDirectId_thenAnOwnerSeesNothingOfThemAndTheyDoExist() {
        assertThat(countAsSuperuser("SELECT count(*) FROM contact WHERE id = ?", ana.contactId())).isEqualTo(1);
        assertThat(countAsSuperuser("SELECT count(*) FROM vehicle WHERE id = ?", ana.vehicleId())).isEqualTo(1);
        assertThat(countAsSuperuser("SELECT count(*) FROM quote WHERE id = ?", ana.sentId())).isEqualTo(1);
        assertThat(countAsSuperuser("SELECT count(*) FROM quote_item WHERE quote_id = ?", ana.sentId())).isEqualTo(1);
        assertThat(countAsSuperuser("SELECT count(*) FROM quote_public_link WHERE quote_id = ?", ana.sentId())).isEqualTo(1);
        assertThat(countAsSuperuser("SELECT count(*) FROM company_profile WHERE company_id = ?", ana.owner().companyId())).isEqualTo(1);

        assertThat(count(beto.tenant(), "SELECT count(*) FROM contact WHERE id = ?", ana.contactId())).isZero();
        assertThat(count(beto.tenant(), "SELECT count(*) FROM vehicle WHERE id = ?", ana.vehicleId())).isZero();
        assertThat(count(beto.tenant(), "SELECT count(*) FROM quote WHERE id = ?", ana.sentId())).isZero();
        assertThat(count(beto.tenant(), "SELECT count(*) FROM quote_item WHERE quote_id = ?", ana.sentId())).isZero();
        assertThat(count(beto.tenant(), "SELECT count(*) FROM quote_public_link WHERE quote_id = ?", ana.sentId())).isZero();
        assertThat(count(beto.tenant(), "SELECT count(*) FROM company_profile WHERE company_id = ?", ana.owner().companyId())).isZero();
    }

    @Test
    void givenAShopsOwnRows_whenReadingThemAsTheOwner_thenTheyAreVisible() {
        assertThat(count(ana.tenant(), "SELECT count(*) FROM contact WHERE id = ?", ana.contactId())).isEqualTo(1);
        assertThat(count(ana.tenant(), "SELECT count(*) FROM quote WHERE id = ?", ana.sentId())).isEqualTo(1);
        assertThat(count(ana.tenant(), "SELECT count(*) FROM quote_item WHERE quote_id = ?", ana.sentId())).isEqualTo(1);
    }

    @Test
    void givenAnotherShopsQuote_whenTryingToChangeOrInsertForIt_thenNothingChangesAndTheInsertIsRefused() {
        long changed = as(beto.tenant(), () -> jdbc.sql("UPDATE quote SET status = 'CANCELLED' WHERE id = ?").param(ana.draftId()).update());

        assertThat(changed).isZero();
        assertThat(countAsSuperuser("SELECT count(*) FROM quote WHERE id = ? AND status = 'DRAFT'", ana.draftId())).isEqualTo(1);
        assertThatThrownBy(() -> as(beto.tenant(), () -> jdbc.sql("""
                INSERT INTO contact (id, company_id, name, phone_ciphertext, phone_blind_index, created_at)
                VALUES (?, ?, 'Intruso', '\\x00'::bytea, 'x', now())
                """).param(UUID.randomUUID()).param(ana.owner().companyId()).update())).isInstanceOf(DataAccessException.class);
    }

    @Test
    void givenTheApplicationRole_whenDeletingAnything_thenTheDatabaseRefuses() {
        for (String table : TENANT_TABLES) {
            assertThatThrownBy(() -> as(ana.tenant(), () -> jdbc.sql("DELETE FROM " + table).update()))
                    .as("DELETE on %s", table).isInstanceOf(DataAccessException.class);
        }
    }

    @Test
    void givenNoTenantScope_whenReadingTheBusinessTables_thenNothingIsVisible() {
        List<AccessScope> scopes = List.of(new AccessScope.Anonymous(), new AccessScope.CredentialLookup(Email.of("qualquer@exemplo.test")),
                new AccessScope.RefreshTokenLookup(new TokenHash("x")));
        for (AccessScope scope : scopes) {
            for (String table : TENANT_TABLES) {
                assertThat(count(scope, "SELECT count(*) FROM " + table)).as("%s em %s", table, scope).isZero();
            }
            assertThat(count(scope, "SELECT count(*) FROM company_profile")).isZero();
        }
    }

    // ---------------------------------------------------------------- escopo do link

    @Test
    void givenALinkScope_whenReadingEverything_thenOnlyTheLinkWithThatHashIsVisible() {
        AccessScope scope = new AccessScope.QuoteLinkLookup(hashOf(ana));

        assertThat(count(scope, "SELECT count(*) FROM quote_public_link")).isEqualTo(1);
        for (String table : List.of("contact", "vehicle", "quote", "quote_item")) {
            assertThat(count(scope, "SELECT count(*) FROM " + table)).as(table).isZero();
        }
        assertThat(count(new AccessScope.QuoteLinkLookup(new TokenHash("hash-que-nao-existe")), "SELECT count(*) FROM quote_public_link")).isZero();
    }

    @Test
    void givenALinkScope_whenChangingAnything_thenTheDatabaseDoesNotAllowIt() {
        AccessScope scope = new AccessScope.QuoteLinkLookup(hashOf(ana));

        assertThat(as(scope, () -> jdbc.sql("UPDATE quote_public_link SET decided_at = now()").update())).isZero();
        assertThat(as(scope, () -> jdbc.sql("UPDATE quote SET status = 'APPROVED'").update())).isZero();
    }

    // ---------------------------------------------------------------- escopo da decisão

    @Test
    void givenADecisionScope_whenReading_thenOnlyThatQuoteAndWhatThePublicPageNeedsAreVisible() {
        AccessScope scope = decisionFor(ana, ana.sentId());

        assertThat(count(scope, "SELECT count(*) FROM quote")).isEqualTo(1);
        assertThat(count(scope, "SELECT count(*) FROM quote_item")).isEqualTo(1);
        assertThat(count(scope, "SELECT count(*) FROM vehicle")).isEqualTo(1);
        assertThat(count(scope, "SELECT count(*) FROM quote_public_link")).isEqualTo(1);
        assertThat(count(scope, "SELECT count(*) FROM company_profile")).isEqualTo(1);
        assertThat(count(scope, "SELECT count(*) FROM company")).isEqualTo(1);
        assertThat(count(scope, "SELECT count(*) FROM contact")).isZero();
    }

    @Test
    void givenADecisionScopeForTheDraftOfTheSameShop_whenReading_thenTheSentQuoteIsNotVisible() {
        AccessScope scope = decisionFor(ana, ana.draftId());

        assertThat(count(scope, "SELECT count(*) FROM quote WHERE id = ?", ana.sentId())).isZero();
        assertThat(count(scope, "SELECT count(*) FROM quote_public_link")).isZero();
    }

    @Test
    void givenAForgedDecisionScope_whenMixingShopsAndQuotes_thenNothingIsVisible() {
        assertThat(count(decisionFor(beto, ana.sentId()), "SELECT count(*) FROM quote")).isZero();
        assertThat(count(decisionFor(ana, beto.sentId()), "SELECT count(*) FROM quote")).isZero();
        assertThat(count(decisionFor(beto, ana.sentId()), "SELECT count(*) FROM quote_item")).isZero();
        assertThat(count(decisionFor(beto, ana.sentId()), "SELECT count(*) FROM vehicle")).isZero();
    }

    @Test
    void givenADecisionScope_whenChanging_thenOnlyTheDecisionColumnsOfThatQuoteCanChange() {
        AccessScope scope = decisionFor(ana, ana.sentId());

        assertThat(as(scope, () -> jdbc.sql("UPDATE quote SET status = 'APPROVED', decided_at = now()").update())).isEqualTo(1);
        assertThat(as(scope, () -> jdbc.sql("UPDATE quote_public_link SET decided_at = now()").update())).isEqualTo(1);
        assertThatThrownBy(() -> as(scope, () -> jdbc.sql("UPDATE quote SET contact_id = ?").param(beto.contactId()).update()))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> as(scope, () -> jdbc.sql("UPDATE quote SET company_id = ?").param(beto.owner().companyId()).update()))
                .isInstanceOf(DataAccessException.class);
        assertThat(countAsSuperuser("SELECT count(*) FROM quote WHERE id = ? AND status = 'DRAFT'", ana.draftId())).isEqualTo(1);
    }

    // ---------------------------------------------------------------- escopo da expiração

    @Test
    void givenTheExpirationScope_whenReading_thenOnlySentQuotesAreVisibleAndNothingPersonal() {
        AccessScope scope = new AccessScope.QuoteExpiration();

        assertThat(count(scope, "SELECT count(*) FROM quote WHERE status = 'DRAFT'")).isZero();
        assertThat(count(scope, "SELECT count(*) FROM quote WHERE status IN ('APPROVED', 'REJECTED', 'CANCELLED')")).isZero();
        assertThat(count(scope, "SELECT count(*) FROM quote WHERE status = 'SENT'")).isGreaterThanOrEqualTo(2);
        for (String table : List.of("contact", "vehicle", "quote_public_link", "company_profile")) {
            assertThat(count(scope, "SELECT count(*) FROM " + table)).as(table).isZero();
        }
    }

    @Test
    void givenTheExpirationScope_whenExpiring_thenOnlyQuotesPastTheirDeadlineAndOnlyToExpiredCanChange() {
        AccessScope scope = new AccessScope.QuoteExpiration();

        long beforeDeadline = as(scope, () -> jdbc.sql("UPDATE quote SET status = 'EXPIRED' WHERE id = ?").param(ana.sentId()).update());
        PostgresTestDatabase.superuserJdbc().sql("UPDATE quote SET expires_at = now() - interval '1 hour' WHERE id = ?").param(ana.sentId()).update();
        long afterDeadline = as(scope, () -> jdbc.sql("UPDATE quote SET status = 'EXPIRED' WHERE id = ?").param(ana.sentId()).update());

        assertThat(beforeDeadline).isZero();
        assertThat(afterDeadline).isEqualTo(1);
        assertThat(countAsSuperuser("SELECT count(*) FROM quote WHERE id = ? AND status = 'EXPIRED'", ana.sentId())).isEqualTo(1);
        PostgresTestDatabase.superuserJdbc().sql("UPDATE quote SET expires_at = now() - interval '1 hour' WHERE id = ?").param(beto.sentId()).update();
        assertThatThrownBy(() -> as(scope, () -> jdbc.sql("UPDATE quote SET status = 'APPROVED' WHERE id = ?").param(beto.sentId()).update()))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void givenTheExpirationScope_whenWritingAudit_thenOnlyAnExpiryEventWithoutAnActorIsAccepted() {
        AccessScope scope = new AccessScope.QuoteExpiration();
        String insert = """
                INSERT INTO audit_event (id, occurred_at, company_id, actor_user_id, event_type, outcome, correlation_id, details)
                VALUES (?, now(), ?, ?, ?, 'SUCCESS', 'c', '{}'::jsonb)
                """;

        assertThat(as(scope, () -> jdbc.sql(insert).param(UUID.randomUUID()).param(ana.owner().companyId()).param(null).param("QUOTE_EXPIRED").update())).isEqualTo(1);
        assertThatThrownBy(() -> as(scope, () -> jdbc.sql(insert).param(UUID.randomUUID()).param(ana.owner().companyId()).param(null).param("QUOTE_SENT").update()))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> as(scope, () -> jdbc.sql(insert).param(UUID.randomUUID()).param(ana.owner().companyId()).param(ana.owner().userId()).param("QUOTE_EXPIRED").update()))
                .isInstanceOf(DataAccessException.class);
    }

    // ---------------------------------------------------------------- nada pessoal em claro

    @Test
    void givenThePersonalData_whenLookingAtEveryStoredRow_thenPhoneEmailAndPlateAreNeverInClear() {
        for (String table : List.of("contact", "vehicle", "quote", "quote_item", "quote_public_link", "company_profile", "company", "audit_event")) {
            for (String needle : List.of("987654321", "ana@cliente.test", "ABC1234", "XYZ9A87", "11933334444")) {
                if (table.equals("company_profile") && needle.equals("11933334444")) {
                    continue; // o telefone comercial também é cifrado; este laço o confere logo abaixo
                }
                assertThat(countAsSuperuser("SELECT count(*) FROM " + table + " t WHERE t::text LIKE ?", "%" + needle + "%"))
                        .as("%s contém %s", table, needle).isZero();
            }
        }
        assertThat(countAsSuperuser("SELECT count(*) FROM company_profile t WHERE t::text LIKE '%11933334444%'")).isZero();
    }
}
