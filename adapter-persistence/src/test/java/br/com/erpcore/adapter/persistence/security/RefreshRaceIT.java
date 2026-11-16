package br.com.erpcore.adapter.persistence.security;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.erpcore.adapter.persistence.support.ApplicationRolePostgresTest;
import br.com.erpcore.adapter.persistence.support.PostgresTestDatabase;
import br.com.erpcore.application.identity.LoginCommand;
import br.com.erpcore.application.identity.LoginUseCase;
import br.com.erpcore.application.identity.RefreshSessionCommand;
import br.com.erpcore.application.identity.RefreshSessionUseCase;
import br.com.erpcore.application.identity.RegisterCompanyAndOwnerCommand;
import br.com.erpcore.application.identity.RegisterCompanyAndOwnerUseCase;
import br.com.erpcore.application.identity.SessionTokens;
import br.com.erpcore.domain.identity.InvalidRefreshTokenException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationRolePostgresTest
class RefreshRaceIT {

    private static final String PASSWORD = "senha-sintetica-12345";
    private static final int ROUNDS = 12;

    @Autowired
    private RegisterCompanyAndOwnerUseCase registerUseCase;
    @Autowired
    private LoginUseCase loginUseCase;
    @Autowired
    private RefreshSessionUseCase refreshUseCase;

    @Test
    void givenTheSameRefreshTokenSentTwiceAtTheSameInstant_whenRefreshing_thenExactlyOneRequestSucceeds() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            for (int round = 0; round < ROUNDS; round++) {
                String email = "corrida-" + UUID.randomUUID() + "@exemplo.test";
                registerUseCase.execute(new RegisterCompanyAndOwnerCommand("Empresa Fictícia", "Dono Fictício", email,
                        PASSWORD, "correlacao-sintetica"));
                SessionTokens session = loginUseCase.execute(new LoginCommand(email, PASSWORD, "correlacao-sintetica"));

                long successes = raceTwiceWith(pool, session.refreshToken());

                assertThat(successes).as("round %d", round).isEqualTo(1);
                assertThat(activeTokensOfTheSession(email)).as("round %d: the reuse revokes the whole session", round).isZero();
            }
        } finally {
            pool.shutdownNow();
        }
    }

    private long raceTwiceWith(ExecutorService pool, String refreshToken) throws Exception {
        CyclicBarrier startTogether = new CyclicBarrier(2);
        Callable<Boolean> attempt = () -> {
            startTogether.await();
            try {
                refreshUseCase.execute(new RefreshSessionCommand(refreshToken, "correlacao-sintetica"));
                return true;
            } catch (InvalidRefreshTokenException rejected) {
                return false;
            }
        };
        List<Future<Boolean>> outcomes = pool.invokeAll(List.of(attempt, attempt));
        long successes = 0;
        for (Future<Boolean> outcome : outcomes) {
            if (outcome.get()) {
                successes++;
            }
        }
        return successes;
    }

    private int activeTokensOfTheSession(String email) {
        return PostgresTestDatabase.superuserJdbc().sql("""
                SELECT count(*) FROM refresh_token t JOIN "user" u ON u.id = t.user_id
                 WHERE u.email = :email AND t.revoked_at IS NULL
                """).param("email", email).query(Integer.class).single();
    }
}
