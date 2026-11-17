package br.com.erpcore.adapter.infra.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.springframework.boot.json.JsonWriter;
import org.springframework.boot.logging.structured.StructuredLoggingJsonMembersCustomizer;

/**
 * Aplica o {@link PersonalDataMasker} a todo valor de texto do log em JSON (mensagem, stack trace, MDC). É o
 * único ponto de mascaramento; ativado por {@code logging.structured.json.customizer}.
 */
public final class PersonalDataMaskingCustomizer implements StructuredLoggingJsonMembersCustomizer<ILoggingEvent> {

    @Override
    public void customize(JsonWriter.Members<ILoggingEvent> members) {
        members.applyingValueProcessor(JsonWriter.ValueProcessor.of(String.class, PersonalDataMasker::mask));
    }
}
