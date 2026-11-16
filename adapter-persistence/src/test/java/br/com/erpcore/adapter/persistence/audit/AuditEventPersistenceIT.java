package br.com.erpcore.adapter.persistence.audit;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.erpcore.adapter.persistence.support.ApplicationRolePostgresTest;
import br.com.erpcore.adapter.persistence.support.PostgresTestDatabase;
import br.com.erpcore.application.audit.port.out.AuditEventRepository;
import br.com.erpcore.application.shared.port.out.AccessScope;
import br.com.erpcore.application.shared.port.out.UnitOfWork;
import br.com.erpcore.domain.audit.AuditEvent;
import br.com.erpcore.domain.audit.AuditEventId;
import br.com.erpcore.domain.audit.AuditEventType;
import br.com.erpcore.domain.audit.AuditOutcome;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationRolePostgresTest
class AuditEventPersistenceIT {

    private static final Instant OCCURRED_AT = Instant.parse("2026-06-01T10:15:30Z");

    @Autowired
    private AuditEventRepository auditEvents;
    @Autowired
    private UnitOfWork unitOfWork;

    @Test
    void givenAnAnonymousEvent_whenSaving_thenEveryFieldIsStoredAndTheDetailsAreRealJson() {
        UUID id = UUID.randomUUID();
        AuditEvent event = new AuditEvent(new AuditEventId(id), OCCURRED_AT, null, null, AuditEventType.LOGIN,
                AuditOutcome.FAILURE, "correlacao-sintetica-9", Map.of("reason", "USER_NOT_FOUND"));

        unitOfWork.execute(new AccessScope.Anonymous(), () -> {
            auditEvents.save(event);
            return null;
        });

        var row = PostgresTestDatabase.superuserJdbc().sql("""
                SELECT occurred_at, company_id, actor_user_id, event_type, outcome, correlation_id,
                       details ->> 'reason' AS reason, jsonb_typeof(details) AS details_type
                  FROM audit_event WHERE id = :id
                """).param("id", id).query().singleRow();
        assertThat(row.get("company_id")).isNull();
        assertThat(row.get("actor_user_id")).isNull();
        assertThat(row.get("event_type")).isEqualTo("LOGIN");
        assertThat(row.get("outcome")).isEqualTo("FAILURE");
        assertThat(row.get("correlation_id")).isEqualTo("correlacao-sintetica-9");
        assertThat(row.get("reason")).isEqualTo("USER_NOT_FOUND");
        assertThat(row.get("details_type")).isEqualTo("object");
    }

    @Test
    void givenDetailsWithSpecialCharacters_whenSaving_thenPostgresParsesThemBackIntact() {
        UUID id = UUID.randomUUID();
        String tricky = "aspas\" barra\\ linha\n tab\t acento ção " + "\u0001";
        AuditEvent event = new AuditEvent(new AuditEventId(id), OCCURRED_AT, null, null, AuditEventType.LOGOUT,
                AuditOutcome.FAILURE, "correlacao-sintetica-10", Map.of("reason", tricky));

        unitOfWork.execute(new AccessScope.Anonymous(), () -> {
            auditEvents.save(event);
            return null;
        });

        var stored = PostgresTestDatabase.superuserJdbc()
                .sql("SELECT details ->> 'reason' FROM audit_event WHERE id = :id").param("id", id)
                .query(String.class).single();
        assertThat(stored).isEqualTo(tricky);
    }

    @Test
    void givenAnEventSavedInATransactionThatFails_whenItRollsBack_thenNothingIsStored() {
        UUID id = UUID.randomUUID();
        AuditEvent event = new AuditEvent(new AuditEventId(id), OCCURRED_AT, null, null, AuditEventType.LOGIN,
                AuditOutcome.FAILURE, "correlacao-sintetica-11", Map.of());

        try {
            unitOfWork.execute(new AccessScope.Anonymous(), () -> {
                auditEvents.save(event);
                throw new IllegalStateException("falha depois de gravar");
            });
        } catch (IllegalStateException expected) {
            // a transação deve ter desfeito a gravação
        }

        var stored = PostgresTestDatabase.superuserJdbc().sql("SELECT count(*) FROM audit_event WHERE id = :id")
                .param("id", id).query(Integer.class).single();
        assertThat(stored).isZero();
    }
}
