package br.com.erpcore.adapter.persistence;

import br.com.erpcore.application.audit.AuditRecorder;
import br.com.erpcore.application.audit.port.out.AuditEventRepository;
import br.com.erpcore.application.company.port.out.CompanyRepository;
import br.com.erpcore.application.identity.GetCurrentContextUseCase;
import br.com.erpcore.application.identity.LoginUseCase;
import br.com.erpcore.application.identity.LogoutUseCase;
import br.com.erpcore.application.identity.RefreshSessionUseCase;
import br.com.erpcore.application.identity.RegisterCompanyAndOwnerUseCase;
import br.com.erpcore.application.identity.SessionIssuer;
import br.com.erpcore.application.identity.port.out.MembershipRepository;
import br.com.erpcore.application.identity.port.out.RefreshTokenRepository;
import br.com.erpcore.application.identity.port.out.UserRepository;
import br.com.erpcore.application.shared.port.out.FieldProtector;
import br.com.erpcore.application.shared.port.out.IdGenerator;
import br.com.erpcore.application.shared.port.out.UnitOfWork;
import br.com.erpcore.application.support.FakeAccessTokenIssuer;
import br.com.erpcore.application.support.FakeFieldProtector;
import br.com.erpcore.application.support.FakePasswordHasher;
import br.com.erpcore.application.support.FakeRefreshTokenCodec;
import br.com.erpcore.domain.identity.LoginLockoutPolicy;
import br.com.erpcore.domain.identity.SessionPolicy;
import br.com.erpcore.application.contact.CreateContactUseCase;
import br.com.erpcore.application.contact.ListContactsUseCase;
import br.com.erpcore.application.contact.port.out.ContactRepository;
import br.com.erpcore.application.identity.OwnerGuard;
import br.com.erpcore.application.profile.GetCompanyProfileUseCase;
import br.com.erpcore.application.profile.UpdateCompanyProfileUseCase;
import br.com.erpcore.application.profile.port.out.CompanyProfileRepository;
import br.com.erpcore.application.quote.ApproveQuoteByLinkUseCase;
import br.com.erpcore.application.quote.CancelQuoteUseCase;
import br.com.erpcore.application.quote.ReviseQuoteItemsUseCase;
import br.com.erpcore.application.quote.ConfirmQuoteUseCase;
import br.com.erpcore.application.quote.CreateQuoteDraftUseCase;
import br.com.erpcore.application.quote.ExpireDueQuotesUseCase;
import br.com.erpcore.application.quote.GetPublicQuoteUseCase;
import br.com.erpcore.application.quote.GetQuoteUseCase;
import br.com.erpcore.application.quote.ListQuotesUseCase;
import br.com.erpcore.application.quote.RejectQuoteByLinkUseCase;
import br.com.erpcore.application.quote.port.out.PublicQuoteUrls;
import br.com.erpcore.application.quote.port.out.QuoteDelivery;
import br.com.erpcore.application.quote.port.out.QuoteLinkCodec;
import br.com.erpcore.application.quote.port.out.QuoteLinkRepository;
import br.com.erpcore.application.quote.port.out.QuoteRepository;
import br.com.erpcore.application.support.FakePublicQuoteUrls;
import br.com.erpcore.application.support.FakeQuoteDelivery;
import br.com.erpcore.application.support.FakeQuoteLinkCodec;
import br.com.erpcore.application.vehicle.RegisterVehicleUseCase;
import br.com.erpcore.application.vehicle.port.out.VehicleRepository;
import java.time.ZoneId;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Monta os casos de uso reais sobre os adaptadores de PostgreSQL. Só o que não é do banco (relógio, ids, hash
 * de senha, tokens) usa os fakes da aplicação.
 */
@Configuration(proxyBeanMethods = false)
class PersistenceTestBeans {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    IdGenerator idGenerator() {
        return UUID::randomUUID;
    }

    @Bean
    FakePasswordHasher passwordHasher() {
        return new FakePasswordHasher();
    }

    @Bean
    FakeRefreshTokenCodec refreshTokenCodec() {
        return new FakeRefreshTokenCodec();
    }

    @Bean
    FakeAccessTokenIssuer accessTokenIssuer(Clock clock) {
        return new FakeAccessTokenIssuer(clock);
    }

    @Bean
    SessionPolicy sessionPolicy() {
        return new SessionPolicy(Duration.ofDays(7), Duration.ofDays(30));
    }

    @Bean
    AuditRecorder auditRecorder(AuditEventRepository events, IdGenerator ids, Clock clock) {
        return new AuditRecorder(events, ids, clock);
    }

    @Bean
    SessionIssuer sessionIssuer(RefreshTokenRepository refreshTokens, FakeRefreshTokenCodec codec,
                                FakeAccessTokenIssuer accessTokens, IdGenerator ids, SessionPolicy policy) {
        return new SessionIssuer(refreshTokens, codec, accessTokens, ids, policy);
    }

    @Bean
    RegisterCompanyAndOwnerUseCase registerUseCase(UnitOfWork unitOfWork, CompanyRepository companies,
                                                   UserRepository users, MembershipRepository memberships,
                                                   FakePasswordHasher hasher, AuditRecorder audit, IdGenerator ids,
                                                   Clock clock) {
        return new RegisterCompanyAndOwnerUseCase(unitOfWork, companies, users, memberships, hasher, audit, ids, clock);
    }

    @Bean
    LoginUseCase loginUseCase(UnitOfWork unitOfWork, UserRepository users, MembershipRepository memberships,
                              FakePasswordHasher hasher, SessionIssuer sessions, AuditRecorder audit, Clock clock) {
        return new LoginUseCase(unitOfWork, users, memberships, hasher, sessions, audit, clock,
                LoginLockoutPolicy.STANDARD);
    }

    @Bean
    RefreshSessionUseCase refreshUseCase(UnitOfWork unitOfWork, RefreshTokenRepository refreshTokens,
                                         MembershipRepository memberships, FakeRefreshTokenCodec codec,
                                         SessionIssuer sessions, AuditRecorder audit, Clock clock) {
        return new RefreshSessionUseCase(unitOfWork, refreshTokens, memberships, codec, sessions, audit, clock);
    }

    @Bean
    LogoutUseCase logoutUseCase(UnitOfWork unitOfWork, RefreshTokenRepository refreshTokens,
                                FakeRefreshTokenCodec codec, AuditRecorder audit, Clock clock) {
        return new LogoutUseCase(unitOfWork, refreshTokens, codec, audit, clock);
    }

    @Bean
    GetCurrentContextUseCase currentContextUseCase(UnitOfWork unitOfWork, UserRepository users,
                                                   CompanyRepository companies, MembershipRepository memberships,
                                                   AuditRecorder audit) {
        return new GetCurrentContextUseCase(unitOfWork, users, companies, memberships, audit);
    }

    @Bean
    FieldProtector fieldProtector() {
        return new FakeFieldProtector();
    }

    @Bean
    OwnerGuard ownerGuard(MembershipRepository memberships) {
        return new OwnerGuard(memberships);
    }

    @Bean
    FakeQuoteLinkCodec quoteLinkCodec() {
        return new FakeQuoteLinkCodec();
    }

    @Bean
    FakeQuoteDelivery quoteDelivery() {
        return new FakeQuoteDelivery();
    }

    @Bean
    PublicQuoteUrls publicQuoteUrls() {
        return new FakePublicQuoteUrls();
    }

    @Bean
    UpdateCompanyProfileUseCase updateProfileUseCase(UnitOfWork unitOfWork, CompanyProfileRepository profiles,
                                                     OwnerGuard guard, AuditRecorder audit) {
        return new UpdateCompanyProfileUseCase(unitOfWork, profiles, guard, audit);
    }

    @Bean
    GetCompanyProfileUseCase getProfileUseCase(UnitOfWork unitOfWork, CompanyProfileRepository profiles,
                                               CompanyRepository companies, OwnerGuard guard) {
        return new GetCompanyProfileUseCase(unitOfWork, profiles, companies, guard);
    }

    @Bean
    CreateContactUseCase createContactUseCase(UnitOfWork unitOfWork, ContactRepository contacts, OwnerGuard guard,
                                              AuditRecorder audit, IdGenerator ids, Clock clock) {
        return new CreateContactUseCase(unitOfWork, contacts, guard, audit, ids, clock);
    }

    @Bean
    ListContactsUseCase listContactsUseCase(UnitOfWork unitOfWork, ContactRepository contacts, OwnerGuard guard) {
        return new ListContactsUseCase(unitOfWork, contacts, guard);
    }

    @Bean
    RegisterVehicleUseCase registerVehicleUseCase(UnitOfWork unitOfWork, ContactRepository contacts,
                                                  VehicleRepository vehicles, OwnerGuard guard, AuditRecorder audit,
                                                  IdGenerator ids, Clock clock) {
        return new RegisterVehicleUseCase(unitOfWork, contacts, vehicles, guard, audit, ids, clock);
    }

    @Bean
    CreateQuoteDraftUseCase createQuoteDraftUseCase(UnitOfWork unitOfWork, ContactRepository contacts,
                                                    VehicleRepository vehicles, QuoteRepository quotes,
                                                    CompanyProfileRepository profiles, OwnerGuard guard,
                                                    AuditRecorder audit, IdGenerator ids, Clock clock) {
        return new CreateQuoteDraftUseCase(unitOfWork, contacts, vehicles, quotes, profiles, guard, audit, ids, clock);
    }

    @Bean
    ConfirmQuoteUseCase confirmQuoteUseCase(UnitOfWork unitOfWork, QuoteRepository quotes, QuoteLinkRepository links,
                                            QuoteLinkCodec codec, PublicQuoteUrls urls, QuoteDelivery delivery,
                                            OwnerGuard guard, AuditRecorder audit, Clock clock) {
        return new ConfirmQuoteUseCase(unitOfWork, quotes, links, codec, urls, delivery, guard, audit, clock,
                ZoneId.of("America/Sao_Paulo"));
    }

    @Bean
    GetQuoteUseCase getQuoteUseCase(UnitOfWork unitOfWork, ContactRepository contacts, VehicleRepository vehicles,
                                    QuoteRepository quotes, OwnerGuard guard, Clock clock) {
        return new GetQuoteUseCase(unitOfWork, contacts, vehicles, quotes, guard, clock);
    }

    @Bean
    ListQuotesUseCase listQuotesUseCase(UnitOfWork unitOfWork, ContactRepository contacts, VehicleRepository vehicles,
                                        QuoteRepository quotes, OwnerGuard guard, Clock clock) {
        return new ListQuotesUseCase(unitOfWork, contacts, vehicles, quotes, guard, clock);
    }

    @Bean
    GetPublicQuoteUseCase getPublicQuoteUseCase(UnitOfWork unitOfWork, QuoteRepository quotes,
                                                QuoteLinkRepository links, QuoteLinkCodec codec,
                                                VehicleRepository vehicles, CompanyProfileRepository profiles,
                                                CompanyRepository companies, Clock clock) {
        return new GetPublicQuoteUseCase(unitOfWork, quotes, links, codec, vehicles, profiles, companies, clock);
    }

    @Bean
    ApproveQuoteByLinkUseCase approveQuoteByLinkUseCase(UnitOfWork unitOfWork, QuoteRepository quotes,
                                                        QuoteLinkRepository links, QuoteLinkCodec codec,
                                                        AuditRecorder audit, Clock clock) {
        return new ApproveQuoteByLinkUseCase(unitOfWork, quotes, links, codec, audit, clock);
    }

    @Bean
    RejectQuoteByLinkUseCase rejectQuoteByLinkUseCase(UnitOfWork unitOfWork, QuoteRepository quotes,
                                                      QuoteLinkRepository links, QuoteLinkCodec codec,
                                                      AuditRecorder audit, Clock clock) {
        return new RejectQuoteByLinkUseCase(unitOfWork, quotes, links, codec, audit, clock);
    }

    @Bean
    ExpireDueQuotesUseCase expireDueQuotesUseCase(UnitOfWork unitOfWork, QuoteRepository quotes, AuditRecorder audit,
                                                  Clock clock) {
        return new ExpireDueQuotesUseCase(unitOfWork, quotes, audit, clock, 50);
    }

    @Bean
    ReviseQuoteItemsUseCase reviseQuoteItemsUseCase(UnitOfWork unitOfWork, ContactRepository contacts,
                                                    VehicleRepository vehicles, QuoteRepository quotes,
                                                    OwnerGuard guard, AuditRecorder audit, Clock clock) {
        return new ReviseQuoteItemsUseCase(unitOfWork, contacts, vehicles, quotes, guard, audit, clock);
    }

    @Bean
    CancelQuoteUseCase cancelQuoteUseCase(UnitOfWork unitOfWork, ContactRepository contacts, VehicleRepository vehicles,
                                          QuoteRepository quotes, OwnerGuard guard, AuditRecorder audit, Clock clock) {
        return new CancelQuoteUseCase(unitOfWork, contacts, vehicles, quotes, guard, audit, clock);
    }
}
