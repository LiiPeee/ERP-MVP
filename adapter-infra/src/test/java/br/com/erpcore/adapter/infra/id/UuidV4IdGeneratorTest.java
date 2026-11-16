package br.com.erpcore.adapter.infra.id;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UuidV4IdGeneratorTest {

    private final UuidV4IdGenerator generator = new UuidV4IdGenerator();

    @Test
    void givenTheGenerator_whenCreatingIds_thenEachIsARandomVersion4Uuid() {
        UUID id = generator.newId();

        assertThat(id.version()).isEqualTo(4);
        assertThat(id.variant()).isEqualTo(2);
    }

    @Test
    void givenTheGenerator_whenCreatingManyIds_thenNoneRepeatsAndNoneIsSequential() {
        Set<UUID> seen = new HashSet<>();
        for (int index = 0; index < 1000; index++) {
            seen.add(generator.newId());
        }

        assertThat(seen).hasSize(1000);
        assertThat(generator.newId().getMostSignificantBits()).isNotEqualTo(generator.newId().getMostSignificantBits());
    }
}
