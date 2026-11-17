package br.com.erpcore.domain.identity;

import java.time.Instant;
import java.util.UUID;

/** Constrói usuários sintéticos e claramente falsos para os testes. */
public final class UserTestBuilder {

    public static final Instant CREATED_AT = Instant.parse("2026-01-01T09:00:00Z");

    private UserId id = new UserId(UUID.fromString("00000000-0000-0000-0000-0000000000a1"));
    private Email email = Email.of("ana.ficticia@exemplo.test");
    private DisplayName displayName = DisplayName.of("Ana Fictícia");
    private PasswordHash passwordHash = new PasswordHash("hash-sintetico-da-senha");

    private UserTestBuilder() {
    }

    public static UserTestBuilder aUser() {
        return new UserTestBuilder();
    }

    public UserTestBuilder withId(UserId id) {
        this.id = id;
        return this;
    }

    public UserTestBuilder withEmail(Email email) {
        this.email = email;
        return this;
    }

    public User build() {
        return User.register(id, email, displayName, passwordHash, CREATED_AT);
    }
}
