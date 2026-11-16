package br.com.erpcore.application.contract;

import static br.com.erpcore.application.contract.ContractData.NOW;
import static br.com.erpcore.application.contract.ContractData.company;
import static br.com.erpcore.application.contract.ContractData.refreshToken;
import static br.com.erpcore.application.contract.ContractData.user;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.erpcore.application.identity.port.out.RefreshTokenRepository;
import br.com.erpcore.domain.company.Company;
import br.com.erpcore.domain.identity.RefreshToken;
import br.com.erpcore.domain.identity.TokenHash;
import br.com.erpcore.domain.identity.User;
import java.time.Duration;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

/** O mesmo contrato roda contra o fake em memória e contra o adaptador de PostgreSQL. */
public abstract class RefreshTokenRepositoryContract {

    private static final long FAMILY_A = 100;
    private static final long FAMILY_B = 200;

    private final User user = user(1);
    private final Company company = company(1);

    protected abstract RefreshTokenRepository repository();

    /** Deixa de pé o usuário e a empresa que os tokens referenciam (chaves estrangeiras no banco real). */
    protected abstract void givenUserAndCompanyExist(User user, Company company);

    protected abstract <T> T inTransaction(Supplier<T> work);

    private void save(RefreshToken token) {
        inTransaction(() -> {
            repository().save(token);
            return null;
        });
    }

    private RefreshToken find(TokenHash hash) {
        return inTransaction(() -> repository().findByHash(hash)).orElseThrow();
    }

    @Test
    void givenASavedToken_whenFindingItByHash_thenEveryFieldComesBack() {
        givenUserAndCompanyExist(user, company);
        RefreshToken saved = refreshToken(1, user, company, FAMILY_A);
        save(saved);

        RefreshToken found = find(saved.tokenHash());

        assertThat(found.id()).isEqualTo(saved.id());
        assertThat(found.userId()).isEqualTo(saved.userId());
        assertThat(found.companyId()).isEqualTo(saved.companyId());
        assertThat(found.familyId()).isEqualTo(saved.familyId());
        assertThat(found.issuedAt()).isEqualTo(saved.issuedAt());
        assertThat(found.expiresAt()).isEqualTo(saved.expiresAt());
        assertThat(found.familyStartedAt()).isEqualTo(saved.familyStartedAt());
        assertThat(found.revokedAt()).isEmpty();
    }

    @Test
    void givenNoSuchToken_whenFindingItByHash_thenTheResultIsEmpty() {
        assertThat(inTransaction(() -> repository().findByHash(new TokenHash("hash-que-nao-existe")))).isEmpty();
    }

    @Test
    void givenASavedToken_whenItIsRevokedAndSavedAgain_thenTheRevocationIsStored() {
        givenUserAndCompanyExist(user, company);
        RefreshToken token = refreshToken(1, user, company, FAMILY_A);
        save(token);
        token.revoke(NOW.plusSeconds(30));

        save(token);

        assertThat(find(token.tokenHash()).revokedAt()).contains(NOW.plusSeconds(30));
    }

    @Test
    void givenARotation_whenSavingTheOldAndTheNewToken_thenBothAreStoredInTheSameFamily() {
        givenUserAndCompanyExist(user, company);
        RefreshToken first = refreshToken(1, user, company, FAMILY_A);
        save(first);
        RefreshToken second = first.rotateTo(new br.com.erpcore.domain.identity.RefreshTokenId(ContractData.uuid(2)),
                new TokenHash("hash-do-token-2"), NOW.plus(Duration.ofHours(1)), ContractData.SESSION_POLICY);

        save(first);
        save(second);

        assertThat(find(first.tokenHash()).isRevoked()).isTrue();
        assertThat(find(second.tokenHash()).isRevoked()).isFalse();
        assertThat(find(second.tokenHash()).familyId()).isEqualTo(first.familyId());
        assertThat(find(second.tokenHash()).familyStartedAt()).isEqualTo(first.familyStartedAt());
    }

    @Test
    void givenTwoFamilies_whenRevokingOne_thenOnlyItsTokensAreRevoked() {
        givenUserAndCompanyExist(user, company);
        RefreshToken inA1 = refreshToken(1, user, company, FAMILY_A);
        RefreshToken inA2 = refreshToken(2, user, company, FAMILY_A);
        RefreshToken inB = refreshToken(3, user, company, FAMILY_B);
        save(inA1);
        save(inA2);
        save(inB);

        inTransaction(() -> {
            repository().revokeFamily(inA1.familyId(), NOW.plusSeconds(60));
            return null;
        });

        assertThat(find(inA1.tokenHash()).revokedAt()).contains(NOW.plusSeconds(60));
        assertThat(find(inA2.tokenHash()).revokedAt()).contains(NOW.plusSeconds(60));
        assertThat(find(inB.tokenHash()).isRevoked()).isFalse();
    }

    @Test
    void givenATokenAlreadyRevoked_whenRevokingItsFamily_thenTheOriginalRevocationInstantIsKept() {
        givenUserAndCompanyExist(user, company);
        RefreshToken early = refreshToken(1, user, company, FAMILY_A);
        RefreshToken other = refreshToken(2, user, company, FAMILY_A);
        early.revoke(NOW.plusSeconds(10));
        save(early);
        save(other);

        inTransaction(() -> {
            repository().revokeFamily(early.familyId(), NOW.plusSeconds(600));
            return null;
        });

        assertThat(find(early.tokenHash()).revokedAt()).contains(NOW.plusSeconds(10));
        assertThat(find(other.tokenHash()).revokedAt()).contains(NOW.plusSeconds(600));
    }
}
