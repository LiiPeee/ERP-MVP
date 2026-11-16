package br.com.erpcore.application.identity;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.erpcore.application.identity.port.out.IssuedAccessToken;
import br.com.erpcore.application.identity.port.out.NewRefreshSecret;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Complementa os testes de cada caso de uso: valores sensíveis nunca saem em texto de log. */
class SecretsNotPrintedTest {

    @Test
    void givenTheCurrentContext_whenPrintingIt_thenPersonalDataDoesNotAppear() {
        CurrentContext context = new CurrentContext(UUID.randomUUID(), "ana.ficticia@exemplo.test", "Ana Fictícia",
                UUID.randomUUID(), "Padaria Fictícia Ltda", "OWNER");

        assertThat(context.toString()).doesNotContain("ana.ficticia").doesNotContain("Ana").doesNotContain("Padaria");
    }

    @Test
    void givenANewRefreshSecret_whenPrintingIt_thenNeitherTheTokenNorItsHashAppears() {
        NewRefreshSecret secret = new NewRefreshSecret("valor-secreto-sintetico", "hash-secreto-sintetico");

        assertThat(secret.toString()).doesNotContain("secreto");
    }

    @Test
    void givenAnIssuedAccessToken_whenPrintingIt_thenTheTokenDoesNotAppear() {
        IssuedAccessToken token = new IssuedAccessToken("jwt-sintetico-secreto", Instant.parse("2026-01-01T00:00:00Z"));

        assertThat(token.toString()).doesNotContain("jwt-sintetico");
    }
}
