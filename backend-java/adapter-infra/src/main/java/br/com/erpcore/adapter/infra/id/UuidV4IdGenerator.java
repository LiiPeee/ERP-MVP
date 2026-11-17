package br.com.erpcore.adapter.infra.id;

import br.com.erpcore.application.shared.port.out.IdGenerator;
import java.util.UUID;

public final class UuidV4IdGenerator implements IdGenerator {

    @Override
    public UUID newId() {
        return UUID.randomUUID();
    }
}
