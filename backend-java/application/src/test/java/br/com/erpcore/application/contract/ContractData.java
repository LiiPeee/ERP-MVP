package br.com.erpcore.application.contract;

import br.com.erpcore.domain.company.Company;
import br.com.erpcore.domain.company.CompanyId;
import br.com.erpcore.domain.company.CompanyName;
import br.com.erpcore.domain.identity.DisplayName;
import br.com.erpcore.domain.identity.Email;
import br.com.erpcore.domain.identity.Membership;
import br.com.erpcore.domain.identity.MembershipId;
import br.com.erpcore.domain.identity.PasswordHash;
import br.com.erpcore.domain.identity.RefreshToken;
import br.com.erpcore.domain.identity.RefreshTokenId;
import br.com.erpcore.domain.identity.Role;
import br.com.erpcore.domain.identity.SessionPolicy;
import br.com.erpcore.domain.identity.TokenFamilyId;
import br.com.erpcore.domain.identity.TokenHash;
import br.com.erpcore.domain.identity.User;
import br.com.erpcore.domain.identity.UserId;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/** Dados sintéticos e claramente falsos para os testes de contrato, que rodam também contra o banco real. */
public final class ContractData {

    public static final Instant NOW = Instant.parse("2026-05-10T15:30:00Z");
    public static final SessionPolicy SESSION_POLICY = new SessionPolicy(Duration.ofDays(7), Duration.ofDays(30));

    private ContractData() {
    }

    public static UUID uuid(long number) {
        return new UUID(0L, number);
    }

    public static User user(long number) {
        return User.register(new UserId(uuid(number)), Email.of("usuario" + number + "@exemplo.test"),
                DisplayName.of("Usuário Fictício " + number), new PasswordHash("hash-sintetico-" + number), NOW);
    }

    public static Company company(long number) {
        return new Company(new CompanyId(uuid(number)), CompanyName.of("Empresa Fictícia " + number), NOW);
    }

    public static Membership membership(long number, Company company, User user) {
        return new Membership(new MembershipId(uuid(number)), company.id(), user.id(), Role.OWNER, NOW);
    }

    public static RefreshToken refreshToken(long number, User user, Company company, long familyNumber) {
        return RefreshToken.first(new RefreshTokenId(uuid(number)), user.id(), company.id(),
                new TokenFamilyId(uuid(familyNumber)), new TokenHash("hash-do-token-" + number), NOW, SESSION_POLICY);
    }
}
