package br.com.erpcore.domain.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.erpcore.domain.company.CompanyId;
import br.com.erpcore.domain.identity.UserId;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuditEventTest {

    private static final Instant NOW = Instant.parse("2026-03-01T08:00:00Z");
    private static final AuditEventId ID = new AuditEventId(UUID.fromString("00000000-0000-0000-0000-0000000000e1"));
    private static final String CORRELATION_ID = "correlacao-sintetica-1";

    @Test
    void givenAnEventWithoutCompanyOrActor_whenReadingThem_thenTheyAreEmpty() {
        AuditEvent event = new AuditEvent(ID, NOW, null, null, AuditEventType.LOGIN, AuditOutcome.FAILURE,
                CORRELATION_ID, Map.of("reason", "USER_NOT_FOUND"));

        assertThat(event.companyId()).isEmpty();
        assertThat(event.actorUserId()).isEmpty();
        assertThat(event.details()).containsEntry("reason", "USER_NOT_FOUND");
    }

    @Test
    void givenAnEventWithCompanyAndActor_whenReadingThem_thenTheyArePresent() {
        CompanyId company = new CompanyId(UUID.fromString("00000000-0000-0000-0000-0000000000c1"));
        UserId actor = new UserId(UUID.fromString("00000000-0000-0000-0000-0000000000a1"));

        AuditEvent event = new AuditEvent(ID, NOW, company, actor, AuditEventType.LOGOUT, AuditOutcome.SUCCESS,
                CORRELATION_ID, Map.of());

        assertThat(event.companyId()).contains(company);
        assertThat(event.actorUserId()).contains(actor);
    }

    @Test
    void givenTheMapUsedToBuildTheEvent_whenItChangesLater_thenTheEventIsNotAffected() {
        Map<String, String> details = new HashMap<>();
        details.put("reason", "BAD_PASSWORD");
        AuditEvent event = new AuditEvent(ID, NOW, null, null, AuditEventType.LOGIN, AuditOutcome.FAILURE,
                CORRELATION_ID, details);

        details.put("email", "alguem@exemplo.test");

        assertThat(event.details()).containsOnlyKeys("reason");
        assertThatThrownBy(() -> event.details().put("x", "y")).isInstanceOf(UnsupportedOperationException.class);
    }
}
