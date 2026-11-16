package br.com.erpcore.adapter.persistence.audit;

import br.com.erpcore.application.audit.port.out.AuditEventRepository;
import br.com.erpcore.domain.audit.AuditEvent;
import jakarta.persistence.EntityManager;

/**
 * A auditoria só aceita inserção: a aplicação não tem permissão de leitura, alteração nem remoção nessa
 * tabela. Por isso grava com {@code persist}, que não consulta a linha antes.
 */
public final class JpaAuditEventRepository implements AuditEventRepository {

    private final EntityManager entityManager;

    public JpaAuditEventRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void save(AuditEvent event) {
        entityManager.persist(new AuditEventJpaEntity(event.id().value(), event.occurredAt(),
                event.companyId().map(id -> id.value()).orElse(null),
                event.actorUserId().map(id -> id.value()).orElse(null),
                event.type().name(), event.outcome().name(), event.correlationId(),
                DetailsJson.toJson(event.details())));
    }
}
