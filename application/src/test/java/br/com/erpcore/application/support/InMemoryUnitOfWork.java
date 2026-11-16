package br.com.erpcore.application.support;

import br.com.erpcore.application.shared.port.out.AccessScope;
import br.com.erpcore.application.shared.port.out.UnitOfWork;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Registra o escopo de cada transação e se terminou em commit ou rollback. Não desfaz dados no rollback:
 * quem testa o desfazer de verdade é o teste de integração da persistência.
 */
public final class InMemoryUnitOfWork implements UnitOfWork {

    private final List<AccessScope> scopes = new ArrayList<>();
    private int commits;
    private int rollbacks;
    private boolean active;

    @Override
    public <T> T execute(AccessScope scope, Supplier<T> work) {
        if (active) {
            throw new IllegalStateException("nested unit of work");
        }
        scopes.add(scope);
        active = true;
        try {
            T result = work.get();
            commits++;
            return result;
        } catch (RuntimeException failure) {
            rollbacks++;
            throw failure;
        } finally {
            active = false;
        }
    }

    /** Os repositórios em memória chamam isto para provar que só são usados dentro de uma transação. */
    void requireActive() {
        if (!active) {
            throw new IllegalStateException("repository used outside a unit of work");
        }
    }

    public List<AccessScope> scopes() {
        return List.copyOf(scopes);
    }

    public int commits() {
        return commits;
    }

    public int rollbacks() {
        return rollbacks;
    }
}
