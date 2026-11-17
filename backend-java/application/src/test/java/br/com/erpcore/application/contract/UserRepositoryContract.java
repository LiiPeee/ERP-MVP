package br.com.erpcore.application.contract;

import static br.com.erpcore.application.contract.ContractData.NOW;
import static br.com.erpcore.application.contract.ContractData.user;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.erpcore.application.identity.port.out.UserRepository;
import br.com.erpcore.domain.identity.Email;
import br.com.erpcore.domain.identity.LoginLockoutPolicy;
import br.com.erpcore.domain.identity.User;
import br.com.erpcore.domain.identity.UserId;
import java.time.Duration;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

/** O mesmo contrato roda contra o fake em memória e contra o adaptador de PostgreSQL. */
public abstract class UserRepositoryContract {

    private static final LoginLockoutPolicy POLICY = new LoginLockoutPolicy(2, Duration.ofMinutes(1), Duration.ofHours(1));

    protected abstract UserRepository repository();

    protected abstract <T> T inTransaction(Supplier<T> work);

    @Test
    void givenASavedUser_whenFindingItById_thenEveryFieldComesBack() {
        User saved = user(1);
        inTransaction(() -> {
            repository().save(saved);
            return null;
        });

        User found = inTransaction(() -> repository().findById(saved.id())).orElseThrow();

        assertThat(found.id()).isEqualTo(saved.id());
        assertThat(found.email()).isEqualTo(saved.email());
        assertThat(found.displayName()).isEqualTo(saved.displayName());
        assertThat(found.passwordHash()).isEqualTo(saved.passwordHash());
        assertThat(found.createdAt()).isEqualTo(saved.createdAt());
        assertThat(found.failedLoginCount()).isZero();
        assertThat(found.lockedUntil()).isEmpty();
    }

    @Test
    void givenASavedUser_whenFindingItByEmail_thenItIsFound() {
        User saved = user(1);
        inTransaction(() -> {
            repository().save(saved);
            return null;
        });

        assertThat(inTransaction(() -> repository().findByEmail(Email.of("USUARIO1@exemplo.test"))))
                .map(User::id).contains(saved.id());
    }

    @Test
    void givenNoSuchUser_whenFinding_thenTheResultIsEmpty() {
        assertThat(inTransaction(() -> repository().findById(new UserId(UUID.randomUUID())))).isEmpty();
        assertThat(inTransaction(() -> repository().findByEmail(Email.of("ninguem@exemplo.test")))).isEmpty();
    }

    @Test
    void givenASavedUser_whenSavingItAgainWithChanges_thenTheStoredStateIsUpdated() {
        User user = user(1);
        inTransaction(() -> {
            repository().save(user);
            return null;
        });
        user.registerFailedLogin(NOW, POLICY);
        user.registerFailedLogin(NOW, POLICY);

        inTransaction(() -> {
            repository().save(user);
            return null;
        });

        User found = inTransaction(() -> repository().findById(user.id())).orElseThrow();
        assertThat(found.failedLoginCount()).isEqualTo(2);
        assertThat(found.lockedUntil()).contains(NOW.plus(Duration.ofMinutes(1)));
    }

    @Test
    void givenAUserReadFromTheStore_whenItIsChangedWithoutSaving_thenTheStoredStateDoesNotChange() {
        User saved = user(1);
        inTransaction(() -> {
            repository().save(saved);
            return null;
        });
        User read = inTransaction(() -> repository().findById(saved.id())).orElseThrow();

        read.registerFailedLogin(NOW, POLICY);

        User again = inTransaction(() -> repository().findById(saved.id())).orElseThrow();
        assertThat(again.failedLoginCount()).isZero();
    }
}
