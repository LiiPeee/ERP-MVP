package br.com.erpcore.bootstrap;

import br.com.erpcore.adapter.infra.id.UuidV4IdGenerator;
import br.com.erpcore.adapter.infra.security.Argon2PasswordHasher;
import br.com.erpcore.adapter.infra.security.JwtAccessTokenIssuer;
import br.com.erpcore.adapter.infra.security.JwtDecoderFactory;
import br.com.erpcore.adapter.infra.security.JwtSettings;
import br.com.erpcore.adapter.infra.security.Sha256RefreshTokenCodec;
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
@EnableConfigurationProperties({JwtProperties.class, SessionProperties.class})
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
}
