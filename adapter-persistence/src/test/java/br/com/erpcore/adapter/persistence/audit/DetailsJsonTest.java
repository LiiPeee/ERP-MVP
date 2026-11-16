package br.com.erpcore.adapter.persistence.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class DetailsJsonTest {

    @Test
    void givenNoDetails_whenConverting_thenItIsAnEmptyObject() {
        assertThat(DetailsJson.toJson(Map.of())).isEqualTo("{}");
    }

    @Test
    void givenSeveralDetails_whenConverting_thenKeysComeInAlphabeticalOrder() {
        assertThat(DetailsJson.toJson(Map.of("reason", "BAD_PASSWORD", "attempt", "3")))
                .isEqualTo("{\"attempt\":\"3\",\"reason\":\"BAD_PASSWORD\"}");
    }

    @Test
    void givenQuotesBackslashesAndControlCharacters_whenConverting_thenTheyAreEscaped() {
        String json = DetailsJson.toJson(Map.of("k", "aspas\" barra\\ linha\n tab\t cr\r nulo\u0000 sino\u0007"));

        assertThat(json).isEqualTo("{\"k\":\"aspas\\\" barra\\\\ linha\\n tab\\t cr\\r nulo\\u0000 sino\\u0007\"}");
    }

    @Test
    void givenAccentedText_whenConverting_thenItIsKeptAsIs() {
        assertThat(DetailsJson.toJson(Map.of("motivo", "não encontrado"))).isEqualTo("{\"motivo\":\"não encontrado\"}");
    }
}
