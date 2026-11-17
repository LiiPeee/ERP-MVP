package br.com.erpcore.adapter.web.correlation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LoggablePathTest {

    private static final String TOKEN = "k3J9x_Qw-7ZpL0aVbN2mC8dR5tYhGfE1uIoS4xWzAqM";

    @Test
    void givenThePublicQuoteApiPath_whenLogging_thenTheTokenSegmentIsHidden() {
        assertThat(LoggablePath.of("/api/public/quotes/" + TOKEN)).isEqualTo("/api/public/quotes/{token}");
        assertThat(LoggablePath.of("/api/public/quotes/" + TOKEN + "/approve")).isEqualTo("/api/public/quotes/{token}/approve");
        assertThat(LoggablePath.of("/api/public/quotes/" + TOKEN + "/reject")).isEqualTo("/api/public/quotes/{token}/reject");
    }

    @Test
    void givenTheClientPagePath_whenLogging_thenTheTokenSegmentIsHidden() {
        assertThat(LoggablePath.of("/o/" + TOKEN)).isEqualTo("/o/{token}");
    }

    @Test
    void givenAnUnfinishedOrMalformedTokenPath_whenLogging_thenItIsHiddenToo() {
        assertThat(LoggablePath.of("/api/public/quotes/curto")).isEqualTo("/api/public/quotes/{token}");
        assertThat(LoggablePath.of("/o/")).isEqualTo("/o/");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/auth/login", "/api/quotes", "/api/quotes/2848f2cd-ba32-4fa5-9e6b-47196187436d/confirm",
            "/", "/login", "/actuator/health", "/other/path/o/abc"})
    void givenAnyOtherPath_whenLogging_thenItIsUnchanged(String path) {
        assertThat(LoggablePath.of(path)).isEqualTo(path);
    }

    @Test
    void givenNoPath_whenLogging_thenItIsEmpty() {
        assertThat(LoggablePath.of(null)).isEmpty();
    }
}
