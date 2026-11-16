package br.com.erpcore.application.identity.port.out;

import br.com.erpcore.domain.identity.Email;
import br.com.erpcore.domain.identity.User;
import br.com.erpcore.domain.identity.UserId;
import java.util.Optional;

public interface UserRepository {

    Optional<User> findByEmail(Email email);

    Optional<User> findById(UserId id);

    /** Insere o usuário ou atualiza o que já existe com o mesmo id. */
    void save(User user);
}
