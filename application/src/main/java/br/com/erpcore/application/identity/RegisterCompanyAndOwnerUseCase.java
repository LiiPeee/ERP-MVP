package br.com.erpcore.application.identity;

import br.com.erpcore.application.audit.AuditRecorder;
import br.com.erpcore.application.company.port.out.CompanyRepository;
import br.com.erpcore.application.identity.port.out.MembershipRepository;
import br.com.erpcore.application.identity.port.out.PasswordHasher;
import br.com.erpcore.application.identity.port.out.UserRepository;
import br.com.erpcore.application.shared.port.out.AccessScope;
import br.com.erpcore.application.shared.port.out.IdGenerator;
import br.com.erpcore.application.shared.port.out.UnitOfWork;
import br.com.erpcore.domain.audit.AuditEventType;
import br.com.erpcore.domain.company.Company;
import br.com.erpcore.domain.company.CompanyId;
import br.com.erpcore.domain.company.CompanyName;
import br.com.erpcore.domain.identity.DisplayName;
import br.com.erpcore.domain.identity.Email;
import br.com.erpcore.domain.identity.EmailAlreadyRegisteredException;
import br.com.erpcore.domain.identity.Membership;
import br.com.erpcore.domain.identity.MembershipId;
import br.com.erpcore.domain.identity.PasswordHash;
import br.com.erpcore.domain.identity.PlainPassword;
import br.com.erpcore.domain.identity.Role;
import br.com.erpcore.domain.identity.User;
import br.com.erpcore.domain.identity.UserId;
import java.time.Clock;
import java.time.Instant;

public final class RegisterCompanyAndOwnerUseCase {

    private static final String REASON_EMAIL_TAKEN = "EMAIL_ALREADY_REGISTERED";

    private final UnitOfWork unitOfWork;
    private final CompanyRepository companies;
    private final UserRepository users;
    private final MembershipRepository memberships;
    private final PasswordHasher passwordHasher;
    private final AuditRecorder audit;
    private final IdGenerator ids;
    private final Clock clock;

    public RegisterCompanyAndOwnerUseCase(UnitOfWork unitOfWork, CompanyRepository companies, UserRepository users,
                                          MembershipRepository memberships, PasswordHasher passwordHasher,
                                          AuditRecorder audit, IdGenerator ids, Clock clock) {
        this.unitOfWork = unitOfWork;
        this.companies = companies;
        this.users = users;
        this.memberships = memberships;
        this.passwordHasher = passwordHasher;
        this.audit = audit;
        this.ids = ids;
        this.clock = clock;
    }

    public RegisterCompanyAndOwnerResult execute(RegisterCompanyAndOwnerCommand command) {
        CompanyName companyName = CompanyName.of(command.companyName());
        DisplayName ownerName = DisplayName.of(command.ownerName());
        Email email = Email.of(command.email());
        PlainPassword password = PlainPassword.chooseNew(command.password());

        // O hash é calculado antes de consultar o e-mail: cadastro repetido leva o mesmo tempo que um novo.
        PasswordHash passwordHash = new PasswordHash(passwordHasher.hash(password.value()));
        CompanyId companyId = new CompanyId(ids.newId());

        Outcome outcome = unitOfWork.execute(new AccessScope.Registration(companyId, email),
                () -> register(companyId, companyName, ownerName, email, passwordHash, command.correlationId()));

        return switch (outcome) {
            case Outcome.Registered registered ->
                    new RegisterCompanyAndOwnerResult(companyId.value(), registered.ownerId().value());
            case Outcome.EmailTaken taken -> throw new EmailAlreadyRegisteredException();
        };
    }

    private Outcome register(CompanyId companyId, CompanyName companyName, DisplayName ownerName, Email email,
                             PasswordHash passwordHash, String correlationId) {
        if (users.findByEmail(email).isPresent()) {
            audit.failure(AuditEventType.REGISTRATION, null, null, correlationId, REASON_EMAIL_TAKEN);
            return new Outcome.EmailTaken();
        }
        Instant now = clock.instant();
        UserId ownerId = new UserId(ids.newId());
        companies.save(new Company(companyId, companyName, now));
        users.save(User.register(ownerId, email, ownerName, passwordHash, now));
        memberships.save(new Membership(new MembershipId(ids.newId()), companyId, ownerId, Role.OWNER, now));
        audit.success(AuditEventType.REGISTRATION, companyId, ownerId, correlationId);
        return new Outcome.Registered(ownerId);
    }

    private sealed interface Outcome {

        record Registered(UserId ownerId) implements Outcome {
        }

        record EmailTaken() implements Outcome {
        }
    }
}
