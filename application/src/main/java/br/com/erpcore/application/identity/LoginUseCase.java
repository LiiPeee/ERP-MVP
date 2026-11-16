package br.com.erpcore.application.identity;

import br.com.erpcore.application.audit.AuditRecorder;
import br.com.erpcore.application.identity.port.out.MembershipRepository;
import br.com.erpcore.application.identity.port.out.PasswordHasher;
import br.com.erpcore.application.identity.port.out.UserRepository;
import br.com.erpcore.application.shared.port.out.AccessScope;
import br.com.erpcore.application.shared.port.out.UnitOfWork;
import br.com.erpcore.domain.audit.AuditEventType;
import br.com.erpcore.domain.identity.Email;
import br.com.erpcore.domain.identity.InvalidCredentialsException;
import br.com.erpcore.domain.identity.LoginLockoutPolicy;
import br.com.erpcore.domain.identity.Membership;
import br.com.erpcore.domain.identity.PlainPassword;
import br.com.erpcore.domain.identity.User;
import br.com.erpcore.domain.shared.DomainValidationException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Autentica por e-mail e senha. Toda falha de credencial vira a mesma {@link InvalidCredentialsException};
 * o motivo real fica só na auditoria. O trabalho dentro da transação devolve um resultado em vez de lançar,
 * para que o contador de falhas e a auditoria sejam confirmados antes do erro chegar ao cliente.
 */
public final class LoginUseCase {

    private static final String REASON_INVALID_INPUT = "INVALID_INPUT";
    private static final String REASON_USER_NOT_FOUND = "USER_NOT_FOUND";
    private static final String REASON_LOCKED = "LOCKED";
    private static final String REASON_BAD_PASSWORD = "BAD_PASSWORD";
    private static final String REASON_NO_MEMBERSHIP = "NO_MEMBERSHIP";
    private static final String NO_PASSWORD = "";

    private final UnitOfWork unitOfWork;
    private final UserRepository users;
    private final MembershipRepository memberships;
    private final PasswordHasher passwordHasher;
    private final SessionIssuer sessions;
    private final AuditRecorder audit;
    private final Clock clock;
    private final LoginLockoutPolicy lockoutPolicy;

    public LoginUseCase(UnitOfWork unitOfWork, UserRepository users, MembershipRepository memberships,
                        PasswordHasher passwordHasher, SessionIssuer sessions, AuditRecorder audit, Clock clock,
                        LoginLockoutPolicy lockoutPolicy) {
        this.unitOfWork = unitOfWork;
        this.users = users;
        this.memberships = memberships;
        this.passwordHasher = passwordHasher;
        this.sessions = sessions;
        this.audit = audit;
        this.clock = clock;
        this.lockoutPolicy = lockoutPolicy;
    }

    public SessionTokens execute(LoginCommand command) {
        SessionOutcome outcome = parse(command)
                .map(credentials -> authenticate(credentials, command.correlationId()))
                .orElseGet(() -> rejectMalformed(command.correlationId()));

        return switch (outcome) {
            case SessionOutcome.Succeeded succeeded -> succeeded.tokens();
            case SessionOutcome.Rejected rejected -> throw new InvalidCredentialsException();
        };
    }

    private Optional<Credentials> parse(LoginCommand command) {
        try {
            return Optional.of(new Credentials(Email.of(command.email()),
                    PlainPassword.ofLoginAttempt(command.password())));
        } catch (DomainValidationException malformed) {
            return Optional.empty();
        }
    }

    private SessionOutcome rejectMalformed(String correlationId) {
        passwordHasher.spendVerificationTime(NO_PASSWORD);
        return unitOfWork.execute(new AccessScope.Anonymous(), () -> {
            audit.failure(AuditEventType.LOGIN, null, null, correlationId, REASON_INVALID_INPUT);
            return new SessionOutcome.Rejected();
        });
    }

    private SessionOutcome authenticate(Credentials credentials, String correlationId) {
        return unitOfWork.execute(new AccessScope.CredentialLookup(credentials.email()),
                () -> authenticateInTransaction(credentials, correlationId));
    }

    private SessionOutcome authenticateInTransaction(Credentials credentials, String correlationId) {
        String password = credentials.password().value();
        Optional<User> found = users.findByEmail(credentials.email());
        if (found.isEmpty()) {
            passwordHasher.spendVerificationTime(password);
            audit.failure(AuditEventType.LOGIN, null, null, correlationId, REASON_USER_NOT_FOUND);
            return new SessionOutcome.Rejected();
        }

        User user = found.get();
        Instant now = clock.instant();
        if (user.isLockedAt(now)) {
            passwordHasher.spendVerificationTime(password);
            audit.failure(AuditEventType.LOGIN, null, user.id(), correlationId, REASON_LOCKED);
            return new SessionOutcome.Rejected();
        }
        if (!passwordHasher.matches(password, user.passwordHash().value())) {
            user.registerFailedLogin(now, lockoutPolicy);
            users.save(user);
            audit.failure(AuditEventType.LOGIN, null, user.id(), correlationId, REASON_BAD_PASSWORD);
            return new SessionOutcome.Rejected();
        }

        List<Membership> userMemberships = memberships.findByUserId(user.id());
        if (userMemberships.isEmpty()) {
            audit.failure(AuditEventType.LOGIN, null, user.id(), correlationId, REASON_NO_MEMBERSHIP);
            return new SessionOutcome.Rejected();
        }

        // Nesta fase cada usuário pertence a uma única empresa.
        Membership membership = userMemberships.get(0);
        user.registerSuccessfulLogin();
        users.save(user);
        SessionTokens tokens = sessions.startSession(membership, now);
        audit.success(AuditEventType.LOGIN, membership.companyId(), user.id(), correlationId);
        return new SessionOutcome.Succeeded(tokens);
    }

    private record Credentials(Email email, PlainPassword password) {
    }
}
