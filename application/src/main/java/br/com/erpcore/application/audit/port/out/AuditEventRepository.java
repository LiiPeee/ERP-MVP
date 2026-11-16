package br.com.erpcore.application.audit.port.out;

import br.com.erpcore.domain.audit.AuditEvent;

public interface AuditEventRepository {

    void save(AuditEvent event);
}
