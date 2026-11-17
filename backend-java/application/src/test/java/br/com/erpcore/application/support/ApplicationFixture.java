package br.com.erpcore.application.support;

import br.com.erpcore.application.audit.AuditRecorder;
import br.com.erpcore.application.profile.GetCompanyProfileUseCase;
import br.com.erpcore.application.profile.UpdateCompanyProfileUseCase;
import br.com.erpcore.application.contact.CreateContactUseCase;
import br.com.erpcore.application.contact.ListContactsUseCase;
import br.com.erpcore.application.identity.AuthenticatedPrincipal;
import br.com.erpcore.application.identity.GetCurrentContextUseCase;
import br.com.erpcore.application.identity.OwnerGuard;
import br.com.erpcore.application.quote.ApproveQuoteByLinkUseCase;
import br.com.erpcore.application.quote.CancelQuoteUseCase;
import br.com.erpcore.application.quote.ConfirmQuoteUseCase;
import br.com.erpcore.application.quote.CreateQuoteDraftUseCase;
import br.com.erpcore.application.quote.ExpireDueQuotesUseCase;
import br.com.erpcore.application.quote.GetPublicQuoteUseCase;
import br.com.erpcore.application.quote.GetQuoteUseCase;
import br.com.erpcore.application.quote.ListQuotesUseCase;
import br.com.erpcore.application.quote.RejectQuoteByLinkUseCase;
import br.com.erpcore.application.quote.ReviseQuoteItemsUseCase;
import br.com.erpcore.application.quote.SendQuoteUseCase;
import br.com.erpcore.application.vehicle.ListVehiclesUseCase;
import br.com.erpcore.application.vehicle.RegisterVehicleUseCase;
import br.com.erpcore.application.identity.LoginCommand;
import br.com.erpcore.application.identity.LogoutUseCase;
import br.com.erpcore.application.identity.LoginUseCase;
import br.com.erpcore.application.identity.RefreshSessionUseCase;
import br.com.erpcore.application.identity.RegisterCompanyAndOwnerCommand;
import br.com.erpcore.application.identity.RegisterCompanyAndOwnerResult;
import br.com.erpcore.application.identity.RegisterCompanyAndOwnerUseCase;
import br.com.erpcore.application.identity.SessionIssuer;
import br.com.erpcore.application.identity.SessionTokens;
import br.com.erpcore.application.shared.port.out.AccessScope;
import br.com.erpcore.domain.identity.DisplayName;
import br.com.erpcore.domain.identity.Email;
import br.com.erpcore.domain.identity.LoginLockoutPolicy;
import br.com.erpcore.domain.identity.PasswordHash;
import br.com.erpcore.domain.identity.SessionPolicy;
import br.com.erpcore.domain.identity.User;
import br.com.erpcore.domain.identity.UserId;
import java.time.Duration;
import java.time.Instant;

/** Monta os fakes e os casos de uso de um cenário de teste. Cada teste cria o seu: nada é compartilhado. */
public final class ApplicationFixture {

    public static final Instant START = Instant.parse("2026-04-01T12:00:00Z");
    public static final java.time.ZoneId BUSINESS_ZONE = java.time.ZoneId.of("America/Sao_Paulo");
    public static final String CORRELATION_ID = "correlacao-sintetica-1";
    public static final String OWNER_EMAIL = "ana.ficticia@exemplo.test";
    public static final String OWNER_PASSWORD = "senha-sintetica-12345";
    public static final String WRONG_PASSWORD = "senha-errada-sintetica";
    public static final SessionPolicy SESSION_POLICY = new SessionPolicy(Duration.ofDays(7), Duration.ofDays(30));
    public static final LoginLockoutPolicy LOCKOUT_POLICY =
            new LoginLockoutPolicy(3, Duration.ofMinutes(1), Duration.ofMinutes(10));

    public final MutableClock clock = new MutableClock(START);
    public final SequentialIdGenerator ids = new SequentialIdGenerator();
    public final InMemoryUnitOfWork unitOfWork = new InMemoryUnitOfWork();
    public final InMemoryUserRepository users = new InMemoryUserRepository(unitOfWork);
    public final InMemoryMembershipRepository memberships = new InMemoryMembershipRepository(unitOfWork);
    public final InMemoryCompanyRepository companies = new InMemoryCompanyRepository(unitOfWork);
    public final InMemoryRefreshTokenRepository refreshTokens = new InMemoryRefreshTokenRepository(unitOfWork);
    public final InMemoryAuditEventRepository auditEvents = new InMemoryAuditEventRepository(unitOfWork);
    public final FakePasswordHasher passwordHasher = new FakePasswordHasher();
    public final FakeAccessTokenIssuer accessTokens = new FakeAccessTokenIssuer(clock);
    public final FakeRefreshTokenCodec refreshCodec = new FakeRefreshTokenCodec();
    public final AuditRecorder auditRecorder = new AuditRecorder(auditEvents, ids, clock);
    public final SessionIssuer sessionIssuer =
            new SessionIssuer(refreshTokens, refreshCodec, accessTokens, ids, SESSION_POLICY);
    public final InMemoryCompanyProfileRepository profiles = new InMemoryCompanyProfileRepository(unitOfWork);
    public final InMemoryContactRepository contacts = new InMemoryContactRepository(unitOfWork);
    public final InMemoryVehicleRepository vehicles = new InMemoryVehicleRepository(unitOfWork);
    public final OwnerGuard ownerGuard = new OwnerGuard(memberships);

    public GetCompanyProfileUseCase getCompanyProfileUseCase() {
        return new GetCompanyProfileUseCase(unitOfWork, profiles, companies, ownerGuard);
    }

    public UpdateCompanyProfileUseCase updateCompanyProfileUseCase() {
        return new UpdateCompanyProfileUseCase(unitOfWork, profiles, ownerGuard, auditRecorder);
    }

    public CreateContactUseCase createContactUseCase() {
        return new CreateContactUseCase(unitOfWork, contacts, ownerGuard, auditRecorder, ids, clock);
    }

    public ListContactsUseCase listContactsUseCase() {
        return new ListContactsUseCase(unitOfWork, contacts, ownerGuard);
    }

    public RegisterVehicleUseCase registerVehicleUseCase() {
        return new RegisterVehicleUseCase(unitOfWork, contacts, vehicles, ownerGuard, auditRecorder, ids, clock);
    }

    public ListVehiclesUseCase listVehiclesUseCase() {
        return new ListVehiclesUseCase(unitOfWork, contacts, vehicles, ownerGuard);
    }

    public final InMemoryQuoteRepository quotes = new InMemoryQuoteRepository(unitOfWork);
    public final InMemoryQuoteLinkRepository quoteLinks = new InMemoryQuoteLinkRepository(unitOfWork);
    public final FakeQuoteLinkCodec quoteLinkCodec = new FakeQuoteLinkCodec();
    public final FakeQuoteDelivery quoteDelivery = new FakeQuoteDelivery();
    public final FakePublicQuoteUrls publicUrls = new FakePublicQuoteUrls();

    public CreateQuoteDraftUseCase createQuoteDraftUseCase() {
        return new CreateQuoteDraftUseCase(unitOfWork, contacts, vehicles, quotes, profiles, ownerGuard, auditRecorder, ids, clock);
    }

    public ReviseQuoteItemsUseCase reviseQuoteItemsUseCase() {
        return new ReviseQuoteItemsUseCase(unitOfWork, contacts, vehicles, quotes, ownerGuard, auditRecorder, clock);
    }

    public ConfirmQuoteUseCase confirmQuoteUseCase() {
        return new ConfirmQuoteUseCase(unitOfWork, quotes, quoteLinks, quoteLinkCodec, publicUrls, quoteDelivery,
                ownerGuard, auditRecorder, clock, BUSINESS_ZONE);
    }

    public SendQuoteUseCase sendQuoteUseCase() {
        return new SendQuoteUseCase(unitOfWork, quotes, quoteLinks, quoteLinkCodec, publicUrls, quoteDelivery, ownerGuard,
                auditRecorder, clock);
    }

    public CancelQuoteUseCase cancelQuoteUseCase() {
        return new CancelQuoteUseCase(unitOfWork, contacts, vehicles, quotes, ownerGuard, auditRecorder, clock);
    }

    public GetQuoteUseCase getQuoteUseCase() {
        return new GetQuoteUseCase(unitOfWork, contacts, vehicles, quotes, ownerGuard, clock);
    }

    public ListQuotesUseCase listQuotesUseCase() {
        return new ListQuotesUseCase(unitOfWork, contacts, vehicles, quotes, ownerGuard, clock);
    }

    public GetPublicQuoteUseCase getPublicQuoteUseCase() {
        return new GetPublicQuoteUseCase(unitOfWork, quotes, quoteLinks, quoteLinkCodec, vehicles, profiles, companies, clock);
    }

    public ApproveQuoteByLinkUseCase approveQuoteByLinkUseCase() {
        return new ApproveQuoteByLinkUseCase(unitOfWork, quotes, quoteLinks, quoteLinkCodec, auditRecorder, clock);
    }

    public RejectQuoteByLinkUseCase rejectQuoteByLinkUseCase() {
        return new RejectQuoteByLinkUseCase(unitOfWork, quotes, quoteLinks, quoteLinkCodec, auditRecorder, clock);
    }

    public ExpireDueQuotesUseCase expireDueQuotesUseCase(int batchSize) {
        return new ExpireDueQuotesUseCase(unitOfWork, quotes, auditRecorder, clock, batchSize);
    }

    /** Quem está autenticado depois do cadastro: o dono da empresa recém-criada. */
    public AuthenticatedPrincipal principalOf(RegisterCompanyAndOwnerResult registered) {
        return new AuthenticatedPrincipal(registered.userId(), registered.companyId());
    }

    /** Uma segunda oficina, para os testes de isolamento entre empresas. */
    public RegisterCompanyAndOwnerResult registerOtherOwner() {
        return registerUseCase().execute(new RegisterCompanyAndOwnerCommand("Oficina Fictícia ME", "Beto Fictício",
                "beto.ficticio@exemplo.test", "outra-senha-sintetica-1", "correlacao-sintetica-2"));
    }

    public RegisterCompanyAndOwnerUseCase registerUseCase() {
        return new RegisterCompanyAndOwnerUseCase(unitOfWork, companies, users, memberships, passwordHasher,
                auditRecorder, ids, clock);
    }

    public LoginUseCase loginUseCase() {
        return new LoginUseCase(unitOfWork, users, memberships, passwordHasher, sessionIssuer, auditRecorder, clock,
                LOCKOUT_POLICY);
    }

    public RefreshSessionUseCase refreshUseCase() {
        return new RefreshSessionUseCase(unitOfWork, refreshTokens, memberships, refreshCodec, sessionIssuer,
                auditRecorder, clock);
    }

    public LogoutUseCase logoutUseCase() {
        return new LogoutUseCase(unitOfWork, refreshTokens, refreshCodec, auditRecorder, clock);
    }

    public GetCurrentContextUseCase currentContextUseCase() {
        return new GetCurrentContextUseCase(unitOfWork, users, companies, memberships, auditRecorder);
    }

    public static RegisterCompanyAndOwnerCommand validRegistration() {
        return new RegisterCompanyAndOwnerCommand("Padaria Fictícia Ltda", "Ana Fictícia", OWNER_EMAIL,
                OWNER_PASSWORD, CORRELATION_ID);
    }

    public RegisterCompanyAndOwnerResult registerOwner() {
        return registerUseCase().execute(validRegistration());
    }

    public SessionTokens loginAsOwner() {
        return loginUseCase().execute(new LoginCommand(OWNER_EMAIL, OWNER_PASSWORD, CORRELATION_ID));
    }

    /** Cria um usuário que não pertence a nenhuma empresa (situação que o cadastro normal não produz). */
    public UserId seedUserWithoutMembership(String email, String password) {
        UserId id = new UserId(ids.newId());
        unitOfWork.execute(new AccessScope.Anonymous(), () -> {
            users.save(User.register(id, Email.of(email), DisplayName.of("Usuário Fictício"),
                    new PasswordHash(passwordHasher.hash(password)), clock.instant()));
            return null;
        });
        return id;
    }
}
