package br.com.erpcore.bootstrap;

import br.com.erpcore.adapter.infra.id.UuidV4IdGenerator;
import br.com.erpcore.adapter.infra.security.Argon2PasswordHasher;
import br.com.erpcore.adapter.infra.security.JwtAccessTokenIssuer;
import br.com.erpcore.adapter.infra.security.JwtDecoderFactory;
import br.com.erpcore.adapter.infra.security.JwtSettings;
import br.com.erpcore.adapter.infra.security.Sha256RefreshTokenCodec;
import br.com.erpcore.adapter.infra.quote.ConfiguredPublicQuoteUrls;
import br.com.erpcore.adapter.infra.quote.ManualQuoteDelivery;
import br.com.erpcore.adapter.infra.quote.Sha256QuoteLinkCodec;
import br.com.erpcore.adapter.infra.security.FieldEncryptionSettings;
import br.com.erpcore.adapter.infra.security.TinkFieldProtector;
import br.com.erpcore.application.contact.CreateContactUseCase;
import br.com.erpcore.application.contact.ListContactsUseCase;
import br.com.erpcore.application.contact.port.out.ContactRepository;
import br.com.erpcore.application.identity.OwnerGuard;
import br.com.erpcore.application.profile.GetCompanyProfileUseCase;
import br.com.erpcore.application.profile.UpdateCompanyProfileUseCase;
import br.com.erpcore.application.profile.port.out.CompanyProfileRepository;
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
import br.com.erpcore.application.quote.port.out.PublicQuoteUrls;
import br.com.erpcore.application.quote.port.out.QuoteDelivery;
import br.com.erpcore.application.quote.port.out.QuoteLinkCodec;
import br.com.erpcore.application.quote.port.out.QuoteLinkRepository;
import br.com.erpcore.application.quote.port.out.QuoteRepository;
import br.com.erpcore.application.shared.port.out.FieldProtector;
import br.com.erpcore.application.vehicle.ListVehiclesUseCase;
import br.com.erpcore.application.vehicle.RegisterVehicleUseCase;
import br.com.erpcore.application.vehicle.port.out.VehicleRepository;
import br.com.erpcore.application.audit.AuditRecorder;
import br.com.erpcore.application.audit.port.out.AuditEventRepository;
import br.com.erpcore.application.company.port.out.CompanyRepository;
import br.com.erpcore.application.identity.GetCurrentContextUseCase;
import br.com.erpcore.application.identity.LoginUseCase;
import br.com.erpcore.application.identity.LogoutUseCase;
import br.com.erpcore.application.identity.RefreshSessionUseCase;
import br.com.erpcore.application.identity.RegisterCompanyAndOwnerUseCase;
import br.com.erpcore.application.identity.SessionIssuer;
import br.com.erpcore.application.identity.port.out.AccessTokenIssuer;
import br.com.erpcore.application.identity.port.out.MembershipRepository;
import br.com.erpcore.application.identity.port.out.PasswordHasher;
import br.com.erpcore.application.identity.port.out.RefreshTokenCodec;
import br.com.erpcore.application.identity.port.out.RefreshTokenRepository;
import br.com.erpcore.application.identity.port.out.UserRepository;
import br.com.erpcore.application.shared.port.out.IdGenerator;
import br.com.erpcore.application.shared.port.out.UnitOfWork;
import br.com.erpcore.domain.identity.LoginLockoutPolicy;
import br.com.erpcore.domain.identity.SessionPolicy;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/**
 * Liga as portas da aplicação às implementações e registra os casos de uso como beans. As classes de aplicação
 * não têm anotação de framework: quem as conhece é só esta configuração.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({JwtProperties.class, SessionProperties.class, FieldEncryptionProperties.class,
        BusinessProperties.class})
class ApplicationConfiguration {

    /** Relógio com precisão de microssegundos, a mesma do PostgreSQL, para o que se grava ser igual ao que se lê. */
    @Bean
    Clock clock() {
        return Clock.tick(Clock.systemUTC(), Duration.ofNanos(1_000));
    }

    @Bean
    IdGenerator idGenerator() {
        return new UuidV4IdGenerator();
    }

    @Bean
    PasswordHasher passwordHasher() {
        return new Argon2PasswordHasher();
    }

    @Bean
    RefreshTokenCodec refreshTokenCodec() {
        return new Sha256RefreshTokenCodec();
    }

    @Bean
    JwtSettings jwtSettings(JwtProperties properties) {
        Map<String, byte[]> keys = new HashMap<>();
        properties.keys().forEach((id, base64) -> keys.put(id, Base64.getDecoder().decode(base64)));
        return new JwtSettings(properties.issuer(), properties.audience(), properties.accessTokenLifetime(),
                properties.activeKeyId(), keys);
    }

    @Bean
    AccessTokenIssuer accessTokenIssuer(JwtSettings settings, Clock clock) {
        return new JwtAccessTokenIssuer(settings, clock);
    }

    @Bean
    JwtDecoder jwtDecoder(JwtSettings settings, Clock clock) {
        return JwtDecoderFactory.create(settings, clock);
    }

    @Bean
    SessionPolicy sessionPolicy(SessionProperties properties) {
        return new SessionPolicy(properties.refreshTokenLifetime(), properties.maxAge());
    }

    @Bean
    AuditRecorder auditRecorder(AuditEventRepository events, IdGenerator ids, Clock clock) {
        return new AuditRecorder(events, ids, clock);
    }

    @Bean
    SessionIssuer sessionIssuer(RefreshTokenRepository refreshTokens, RefreshTokenCodec codec,
                                AccessTokenIssuer accessTokens, IdGenerator ids, SessionPolicy policy) {
        return new SessionIssuer(refreshTokens, codec, accessTokens, ids, policy);
    }

    @Bean
    RegisterCompanyAndOwnerUseCase registerCompanyAndOwnerUseCase(UnitOfWork unitOfWork, CompanyRepository companies,
                                                                  UserRepository users, MembershipRepository memberships,
                                                                  PasswordHasher hasher, AuditRecorder audit,
                                                                  IdGenerator ids, Clock clock) {
        return new RegisterCompanyAndOwnerUseCase(unitOfWork, companies, users, memberships, hasher, audit, ids, clock);
    }

    @Bean
    LoginUseCase loginUseCase(UnitOfWork unitOfWork, UserRepository users, MembershipRepository memberships,
                              PasswordHasher hasher, SessionIssuer sessions, AuditRecorder audit, Clock clock) {
        return new LoginUseCase(unitOfWork, users, memberships, hasher, sessions, audit, clock,
                LoginLockoutPolicy.STANDARD);
    }

    @Bean
    RefreshSessionUseCase refreshSessionUseCase(UnitOfWork unitOfWork, RefreshTokenRepository refreshTokens,
                                                MembershipRepository memberships, RefreshTokenCodec codec,
                                                SessionIssuer sessions, AuditRecorder audit, Clock clock) {
        return new RefreshSessionUseCase(unitOfWork, refreshTokens, memberships, codec, sessions, audit, clock);
    }

    @Bean
    LogoutUseCase logoutUseCase(UnitOfWork unitOfWork, RefreshTokenRepository refreshTokens, RefreshTokenCodec codec,
                                AuditRecorder audit, Clock clock) {
        return new LogoutUseCase(unitOfWork, refreshTokens, codec, audit, clock);
    }

    @Bean
    GetCurrentContextUseCase getCurrentContextUseCase(UnitOfWork unitOfWork, UserRepository users,
                                                      CompanyRepository companies, MembershipRepository memberships,
                                                      AuditRecorder audit) {
        return new GetCurrentContextUseCase(unitOfWork, users, companies, memberships, audit);
    }

    // ---------------------------------------------------------------- criptografia de campo

    @Bean
    FieldProtector fieldProtector(FieldEncryptionProperties properties) {
        Map<String, byte[]> keys = new HashMap<>();
        properties.keys().forEach((id, base64) -> keys.put(id, Base64.getDecoder().decode(base64)));
        return new TinkFieldProtector(new FieldEncryptionSettings(properties.activeKeyId(), keys,
                Base64.getDecoder().decode(properties.blindIndexKey())));
    }

    // ---------------------------------------------------------------- orçamento

    @Bean
    OwnerGuard ownerGuard(MembershipRepository memberships) {
        return new OwnerGuard(memberships);
    }

    @Bean
    QuoteLinkCodec quoteLinkCodec() {
        return new Sha256QuoteLinkCodec();
    }

    @Bean
    PublicQuoteUrls publicQuoteUrls(BusinessProperties properties) {
        return new ConfiguredPublicQuoteUrls(properties.publicBaseUrl());
    }

    @Bean
    QuoteDelivery quoteDelivery() {
        return new ManualQuoteDelivery();
    }

    @Bean
    GetCompanyProfileUseCase getCompanyProfileUseCase(UnitOfWork unitOfWork, CompanyProfileRepository profiles,
                                                      CompanyRepository companies, OwnerGuard guard) {
        return new GetCompanyProfileUseCase(unitOfWork, profiles, companies, guard);
    }

    @Bean
    UpdateCompanyProfileUseCase updateCompanyProfileUseCase(UnitOfWork unitOfWork, CompanyProfileRepository profiles,
                                                            OwnerGuard guard, AuditRecorder audit) {
        return new UpdateCompanyProfileUseCase(unitOfWork, profiles, guard, audit);
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
    ListVehiclesUseCase listVehiclesUseCase(UnitOfWork unitOfWork, ContactRepository contacts, VehicleRepository vehicles,
                                            OwnerGuard guard) {
        return new ListVehiclesUseCase(unitOfWork, contacts, vehicles, guard);
    }

    @Bean
    CreateQuoteDraftUseCase createQuoteDraftUseCase(UnitOfWork unitOfWork, ContactRepository contacts,
                                                    VehicleRepository vehicles, QuoteRepository quotes,
                                                    CompanyProfileRepository profiles, OwnerGuard guard,
                                                    AuditRecorder audit, IdGenerator ids, Clock clock) {
        return new CreateQuoteDraftUseCase(unitOfWork, contacts, vehicles, quotes, profiles, guard, audit, ids, clock);
    }

    @Bean
    ReviseQuoteItemsUseCase reviseQuoteItemsUseCase(UnitOfWork unitOfWork, ContactRepository contacts,
                                                    VehicleRepository vehicles, QuoteRepository quotes,
                                                    OwnerGuard guard, AuditRecorder audit, Clock clock) {
        return new ReviseQuoteItemsUseCase(unitOfWork, contacts, vehicles, quotes, guard, audit, clock);
    }

    @Bean
    ConfirmQuoteUseCase confirmQuoteUseCase(UnitOfWork unitOfWork, QuoteRepository quotes, QuoteLinkRepository links,
                                            QuoteLinkCodec codec, PublicQuoteUrls urls, QuoteDelivery delivery,
                                            OwnerGuard guard, AuditRecorder audit, Clock clock,
                                            BusinessProperties business) {
        return new ConfirmQuoteUseCase(unitOfWork, quotes, links, codec, urls, delivery, guard, audit, clock, business.zone());
    }

    @Bean
    SendQuoteUseCase sendQuoteUseCase(UnitOfWork unitOfWork, QuoteRepository quotes, QuoteLinkRepository links,
                                      QuoteLinkCodec codec, PublicQuoteUrls urls, QuoteDelivery delivery,
                                      OwnerGuard guard, AuditRecorder audit, Clock clock) {
        return new SendQuoteUseCase(unitOfWork, quotes, links, codec, urls, delivery, guard, audit, clock);
    }

    @Bean
    CancelQuoteUseCase cancelQuoteUseCase(UnitOfWork unitOfWork, ContactRepository contacts, VehicleRepository vehicles,
                                          QuoteRepository quotes, OwnerGuard guard, AuditRecorder audit, Clock clock) {
        return new CancelQuoteUseCase(unitOfWork, contacts, vehicles, quotes, guard, audit, clock);
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

    /** Lotes de 100. Quem chama é a tarefa agendada {@code ExpireDueQuotesJob}. */
    @Bean
    ExpireDueQuotesUseCase expireDueQuotesUseCase(UnitOfWork unitOfWork, QuoteRepository quotes, AuditRecorder audit,
                                                  Clock clock) {
        return new ExpireDueQuotesUseCase(unitOfWork, quotes, audit, clock, 100);
    }
}
