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
import br.com.erpcore.application.shared.port.out.IdGenerator;
import br.com.erpcore.application.shared.port.out.UnitOfWork;
import br.com.erpcore.application.support.FakeAccessTokenIssuer;
import br.com.erpcore.application.support.FakePasswordHasher;
import br.com.erpcore.application.support.FakeRefreshTokenCodec;
import br.com.erpcore.domain.identity.LoginLockoutPolicy;
import br.com.erpcore.domain.identity.SessionPolicy;
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
}
