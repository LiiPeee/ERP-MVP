package br.com.erpcore.adapter.persistence.tenancy;

import br.com.erpcore.application.shared.port.out.AccessScope;
import br.com.erpcore.application.shared.port.out.UnitOfWork;
import java.util.function.Supplier;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Abre a transação, define o escopo de acesso (que as políticas de RLS leem) e só então roda o trabalho. É o
 * único ponto da aplicação que decide o que uma transação enxerga.
 */
public final class PostgresUnitOfWork implements UnitOfWork {

    private final TransactionTemplate transactions;
    private final ScopeVariables scopeVariables;

    public PostgresUnitOfWork(PlatformTransactionManager transactionManager, ScopeVariables scopeVariables) {
        this.transactions = new TransactionTemplate(transactionManager);
        this.scopeVariables = scopeVariables;
    }

    @Override
    public <T> T execute(AccessScope scope, Supplier<T> work) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("nested unit of work");
        }
        return transactions.execute(status -> {
            scopeVariables.apply(scope);
            return work.get();
        });
    }
}
