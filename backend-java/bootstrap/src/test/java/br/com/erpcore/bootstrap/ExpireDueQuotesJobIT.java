package br.com.erpcore.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.erpcore.bootstrap.support.ApplicationHttpTest;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

/** A tarefa que vence os orçamentos fora do prazo está registrada e roda sem erro contra o banco real. */
@ApplicationHttpTest
class ExpireDueQuotesJobIT {

    @Autowired
    private ExpireDueQuotesJob job;

    @Test
    void givenNothingDue_whenTheJobRuns_thenItExpiresNothingAndDoesNotFail() {
        assertThat(job.run()).isZero();
    }

    @Test
    void givenTheJob_whenLookingAtItsSchedule_thenItIsScheduledWithAConfigurableDelay() throws NoSuchMethodException {
        Method run = ExpireDueQuotesJob.class.getDeclaredMethod("run");

        Scheduled schedule = run.getAnnotation(Scheduled.class);

        assertThat(schedule).isNotNull();
        assertThat(schedule.fixedDelayString()).contains("erp.business.expiry-interval");
    }
}
