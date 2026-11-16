package br.com.erpcore.application.support;

import br.com.erpcore.application.audit.AuditRecorder;
import br.com.erpcore.application.identity.LoginCommand;
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
