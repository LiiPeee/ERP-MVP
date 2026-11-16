package br.com.erpcore.adapter.infra.security;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.erpcore.application.identity.port.out.NewRefreshSecret;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class Sha256RefreshTokenCodecTest {

    private final Sha256RefreshTokenCodec codec = new Sha256RefreshTokenCodec();

    @Test
    void givenANewToken_whenGenerating_thenItHasAtLeast256BitsOfUrlSafeText() {
        NewRefreshSecret secret = codec.generate();

        assertThat(secret.value()).matches("[A-Za-z0-9_-]{43,}");
    }

    @Test
    void givenManyTokens_whenGenerating_thenNoneRepeats() {
        Set<String> seen = new HashSet<>();
        for (int index = 0; index < 1000; index++) {
            seen.add(codec.generate().value());
        }

        assertThat(seen).hasSize(1000);
    }

    @Test
    void givenANewToken_whenGenerating_thenItsHashIsTheHashOfItsValue() {
        NewRefreshSecret secret = codec.generate();

        assertThat(secret.hash()).isEqualTo(codec.hash(secret.value()));
        assertThat(secret.hash()).isNotEqualTo(secret.value());
    }

    @Test
    void givenAnyText_whenHashing_thenTheResultIs64LowerCaseHexCharactersAndIsStable() {
        assertThat(codec.hash("qualquer coisa")).matches("[0-9a-f]{64}").isEqualTo(codec.hash("qualquer coisa"));
    }

    @Test
    void givenAKnownInput_whenHashing_thenItMatchesTheSha256TestVector() {
        assertThat(codec.hash("abc")).isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }
}
