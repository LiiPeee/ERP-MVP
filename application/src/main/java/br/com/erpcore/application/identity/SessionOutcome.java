package br.com.erpcore.application.identity;

/** Resultado de uma tentativa de abrir ou renovar sessão, devolvido de dentro da transação para ser confirmado. */
sealed interface SessionOutcome {

    record Succeeded(SessionTokens tokens) implements SessionOutcome {
    }

    record Rejected() implements SessionOutcome {
    }
}
