package br.com.erpcore.adapter.persistence.identity;

import br.com.erpcore.application.identity.port.out.UserRepository;
import br.com.erpcore.domain.identity.DisplayName;
import br.com.erpcore.domain.identity.Email;
import br.com.erpcore.domain.identity.PasswordHash;
import br.com.erpcore.domain.identity.User;
import br.com.erpcore.domain.identity.UserId;
import java.util.Optional;

public final class JpaUserRepository implements UserRepository {

    private final UserJpaRepository jpa;

    public JpaUserRepository(UserJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return jpa.findByEmail(email.value()).map(JpaUserRepository::toDomain);
    }

    @Override
    public Optional<User> findById(UserId id) {
        return jpa.findById(id.value()).map(JpaUserRepository::toDomain);
    }

    @Override
    public void save(User user) {
        jpa.save(new UserJpaEntity(user.id().value(), user.email().value(), user.displayName().value(),
                user.passwordHash().value(), user.failedLoginCount(), user.lockedUntil().orElse(null),
                user.createdAt()));
    }

    private static User toDomain(UserJpaEntity entity) {
        return User.restore(new UserId(entity.getId()), Email.of(entity.getEmail()),
                DisplayName.of(entity.getDisplayName()), new PasswordHash(entity.getPasswordHash()),
                entity.getCreatedAt(), entity.getFailedLoginCount(), entity.getLockedUntil());
    }
}
