package br.com.erpcore.application.support;

import br.com.erpcore.application.identity.port.out.UserRepository;
import br.com.erpcore.domain.identity.Email;
import br.com.erpcore.domain.identity.User;
import br.com.erpcore.domain.identity.UserId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Guarda e devolve cópias, como um banco: alterar o objeto lido não muda nada sem {@code save}. */
public final class InMemoryUserRepository implements UserRepository {

    private final InMemoryUnitOfWork unitOfWork;
    private final Map<UserId, User> storage = new LinkedHashMap<>();

    public InMemoryUserRepository(InMemoryUnitOfWork unitOfWork) {
        this.unitOfWork = unitOfWork;
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        unitOfWork.requireActive();
        return storage.values().stream().filter(user -> user.email().equals(email)).findFirst().map(this::copy);
    }

    @Override
    public Optional<User> findById(UserId id) {
        unitOfWork.requireActive();
        return Optional.ofNullable(storage.get(id)).map(this::copy);
    }

    @Override
    public void save(User user) {
        unitOfWork.requireActive();
        storage.put(user.id(), copy(user));
    }

    /** Leitura direta para os testes conferirem o que foi gravado, fora de qualquer transação. */
    public List<User> all() {
        return new ArrayList<>(storage.values().stream().map(this::copy).toList());
    }

    private User copy(User user) {
        return User.restore(user.id(), user.email(), user.displayName(), user.passwordHash(), user.createdAt(),
                user.failedLoginCount(), user.lockedUntil().orElse(null));
    }
}
