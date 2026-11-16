package br.com.erpcore.application.shared.port.out;

import java.util.function.Supplier;

/**
 * Uma transação. Tudo que o trabalho {@code work} grava ou lê acontece sob o {@code scope}, que diz à
 * persistência quais linhas aquele trabalho pode enxergar. Se {@code work} lançar exceção, nada é gravado;
 * por isso uma falha que precisa deixar rastro (auditoria) é devolvida como resultado, não lançada.
 */
public interface UnitOfWork {

    <T> T execute(AccessScope scope, Supplier<T> work);
}
