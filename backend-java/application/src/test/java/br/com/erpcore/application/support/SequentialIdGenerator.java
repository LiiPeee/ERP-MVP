package br.com.erpcore.application.support;

import br.com.erpcore.application.shared.port.out.IdGenerator;
import java.util.UUID;

/** Gera UUIDs previsíveis (1, 2, 3...) para os testes. */
public final class SequentialIdGenerator implements IdGenerator {

    private long counter;

    @Override
    public UUID newId() {
        counter++;
        return new UUID(0L, counter);
    }
}
