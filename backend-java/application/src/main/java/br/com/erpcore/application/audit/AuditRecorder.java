package br.com.erpcore.application.audit;

import br.com.erpcore.application.audit.port.out.AuditEventRepository;
import br.com.erpcore.application.shared.port.out.IdGenerator;
import br.com.erpcore.domain.audit.AuditEvent;
import br.com.erpcore.domain.audit.AuditEventId;
import br.com.erpcore.domain.audit.AuditEventType;
import br.com.erpcore.domain.audit.AuditOutcome;
import br.com.erpcore.domain.company.CompanyId;
import br.com.erpcore.domain.identity.UserId;
import java.time.Clock;
import java.util.Map;
import java.util.Objects;

/** Monta e grava eventos de auditoria. Precisa ser chamado dentro de uma transação. */
public final class AuditRecorder {

    private static final String REASON_KEY = "reason";

    private final AuditEventRepository events;
    private final IdGenerator ids;
    private final Clock clock;

    public AuditRecorder(AuditEventRepository events, IdGenerator ids, Clock clock) {
        this.events = events;
        this.ids = ids;
        this.clock = clock;
    }

    public void success(AuditEventType type, CompanyId companyId, UserId actorId, String correlationId) {
        record(type, AuditOutcome.SUCCESS, Objects.requireNonNull(companyId), Objects.requireNonNull(actorId),
                correlationId, Map.of());
    }

    /** Sucesso com detalhes técnicos (por exemplo, o id do que foi criado). Nunca dado pessoal. */
    public void success(AuditEventType type, CompanyId companyId, UserId actorId, String correlationId,
                        Map<String, String> details) {
        record(type, AuditOutcome.SUCCESS, Objects.requireNonNull(companyId), Objects.requireNonNull(actorId),
                correlationId, details);
    }

    /** Ação sem usuário autenticado (o cliente pelo link, ou uma tarefa agendada), mas dentro de uma empresa. */
    public void systemSuccess(AuditEventType type, CompanyId companyId, String correlationId,
                              Map<String, String> details) {
        record(type, AuditOutcome.SUCCESS, Objects.requireNonNull(companyId), null, correlationId, details);
    }

    /**
     * Registra uma falha. {@code companyId} e {@code actorId} podem ser {@code null} quando ainda não se sabe
     * quem agiu. {@code reasonCode} é um código técnico, nunca dado pessoal.
     */
    public void failure(AuditEventType type, CompanyId companyId, UserId actorId, String correlationId,
                        String reasonCode) {
        record(type, AuditOutcome.FAILURE, companyId, actorId, correlationId, Map.of(REASON_KEY, reasonCode));
    }

    private void record(AuditEventType type, AuditOutcome outcome, CompanyId companyId, UserId actorId,
                        String correlationId, Map<String, String> details) {
        events.save(new AuditEvent(new AuditEventId(ids.newId()), clock.instant(), companyId, actorId, type,
                outcome, correlationId, details));
    }
}
