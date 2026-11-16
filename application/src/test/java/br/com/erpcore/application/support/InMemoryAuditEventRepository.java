package br.com.erpcore.application.support;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.erpcore.application.audit.port.out.AuditEventRepository;
import br.com.erpcore.domain.audit.AuditEvent;
import java.util.ArrayList;
import java.util.List;

public final class InMemoryAuditEventRepository implements AuditEventRepository {

    private final InMemoryUnitOfWork unitOfWork;
    private final List<AuditEvent> storage = new ArrayList<>();

    public InMemoryAuditEventRepository(InMemoryUnitOfWork unitOfWork) {
        this.unitOfWork = unitOfWork;
    }

    @Override
    public void save(AuditEvent event) {
        unitOfWork.requireActive();
        storage.add(event);
    }

    public List<AuditEvent> all() {
        return List.copyOf(storage);
    }

    /** O evento único que o teste espera; falha se houver zero ou mais de um. */
    public AuditEvent only() {
        assertThat(storage).hasSize(1);
        return storage.get(0);
    }
}
