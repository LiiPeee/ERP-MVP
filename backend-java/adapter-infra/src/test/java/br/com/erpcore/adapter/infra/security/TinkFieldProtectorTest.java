package br.com.erpcore.adapter.infra.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.erpcore.application.shared.port.out.FieldContext;
import br.com.erpcore.application.shared.port.out.FieldProtectionException;
import br.com.erpcore.domain.company.CompanyId;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TinkFieldProtectorTest {

    private static final CompanyId COMPANY_A = new CompanyId(UUID.fromString("00000000-0000-0000-0000-0000000000a1"));
    private static final CompanyId COMPANY_B = new CompanyId(UUID.fromString("00000000-0000-0000-0000-0000000000b2"));
    private static final FieldContext PHONE_OF_A = new FieldContext(COMPANY_A, "contact.phone");
    private static final String PHONE = "+5511987654321";

    private static byte[] key(int seed) {
        byte[] bytes = new byte[32];
        Arrays.fill(bytes, (byte) seed);
        return bytes;
    }

    private static byte[] blindKey(int seed) {
        byte[] bytes = new byte[32];
        Arrays.fill(bytes, (byte) seed);
        return bytes;
    }

    private static FieldEncryptionSettings settings(String activeId, Map<String, byte[]> keys, byte[] blind) {
        return new FieldEncryptionSettings(activeId, keys, blind);
    }

    private static TinkFieldProtector protector() {
        return new TinkFieldProtector(settings("k1", Map.of("k1", key(1)), blindKey(9)));
    }

    // ---------------------------------------------------------------- cifra

    @Test
    void givenAPlaintext_whenEncryptingAndDecrypting_thenTheOriginalComesBack() {
        TinkFieldProtector protector = protector();

        byte[] ciphertext = protector.encrypt(PHONE_OF_A, PHONE);

        assertThat(protector.decrypt(PHONE_OF_A, ciphertext)).isEqualTo(PHONE);
    }

    @Test
    void givenAPlaintext_whenLookingAtTheCiphertext_thenTheValueIsNotInIt() {
        byte[] ciphertext = protector().encrypt(PHONE_OF_A, PHONE);

        assertThat(new String(ciphertext, StandardCharsets.ISO_8859_1)).doesNotContain("987654321");
        assertThat(ciphertext).hasSizeGreaterThan(PHONE.length());
    }

    @Test
    void givenTheSamePlaintextTwice_whenEncrypting_thenTheCiphertextsDiffer() {
        TinkFieldProtector protector = protector();

        assertThat(protector.encrypt(PHONE_OF_A, PHONE)).isNotEqualTo(protector.encrypt(PHONE_OF_A, PHONE));
    }

    @Test
    void givenNonAsciiText_whenEncryptingAndDecrypting_thenItSurvivesIntact() {
        TinkFieldProtector protector = protector();

        assertThat(protector.decrypt(PHONE_OF_A, protector.encrypt(PHONE_OF_A, "José da Conceição"))).isEqualTo("José da Conceição");
    }

    @Test
    void givenACiphertextOfAnotherCompanyOrField_whenDecrypting_thenItIsRefused() {
        TinkFieldProtector protector = protector();
        byte[] ciphertext = protector.encrypt(PHONE_OF_A, PHONE);

        assertThatThrownBy(() -> protector.decrypt(new FieldContext(COMPANY_B, "contact.phone"), ciphertext))
                .isInstanceOf(FieldProtectionException.class);
        assertThatThrownBy(() -> protector.decrypt(new FieldContext(COMPANY_A, "vehicle.plate"), ciphertext))
                .isInstanceOf(FieldProtectionException.class);
    }

    @Test
    void givenATamperedCiphertext_whenDecrypting_thenItIsRefused() {
        TinkFieldProtector protector = protector();
        byte[] ciphertext = protector.encrypt(PHONE_OF_A, PHONE);
        ciphertext[ciphertext.length - 1] ^= 0x01;

        assertThatThrownBy(() -> protector.decrypt(PHONE_OF_A, ciphertext)).isInstanceOf(FieldProtectionException.class);
    }

    @Test
    void givenGarbageOrTooShortBytes_whenDecrypting_thenItIsRefusedWithoutLeakingAnythingElse() {
        TinkFieldProtector protector = protector();

        assertThatThrownBy(() -> protector.decrypt(PHONE_OF_A, new byte[0])).isInstanceOf(FieldProtectionException.class);
        assertThatThrownBy(() -> protector.decrypt(PHONE_OF_A, new byte[] {1, 2, 3})).isInstanceOf(FieldProtectionException.class);
    }

    @Test
    void givenADifferentKey_whenDecrypting_thenItIsRefused() {
        byte[] ciphertext = protector().encrypt(PHONE_OF_A, PHONE);
        TinkFieldProtector other = new TinkFieldProtector(settings("k1", Map.of("k1", key(2)), blindKey(9)));

        assertThatThrownBy(() -> other.decrypt(PHONE_OF_A, ciphertext)).isInstanceOf(FieldProtectionException.class);
    }

    // ---------------------------------------------------------------- rotação de chave

    @Test
    void givenAKeyRotation_whenTheNewKeyBecomesActive_thenOldDataStillDecryptsAndNewDataUsesTheNewKey() {
        TinkFieldProtector before = protector();
        byte[] oldCiphertext = before.encrypt(PHONE_OF_A, PHONE);
        TinkFieldProtector after = new TinkFieldProtector(settings("k2", Map.of("k1", key(1), "k2", key(2)), blindKey(9)));

        byte[] newCiphertext = after.encrypt(PHONE_OF_A, PHONE);

        assertThat(after.decrypt(PHONE_OF_A, oldCiphertext)).isEqualTo(PHONE);
        assertThat(after.decrypt(PHONE_OF_A, newCiphertext)).isEqualTo(PHONE);
        assertThat(Arrays.copyOf(newCiphertext, 5)).isNotEqualTo(Arrays.copyOf(oldCiphertext, 5));
        assertThatThrownBy(() -> before.decrypt(PHONE_OF_A, newCiphertext)).isInstanceOf(FieldProtectionException.class);
    }

    @Test
    void givenTheOldKeyIsRemoved_whenDecryptingOldData_thenItIsRefused() {
        byte[] oldCiphertext = protector().encrypt(PHONE_OF_A, PHONE);
        TinkFieldProtector onlyNew = new TinkFieldProtector(settings("k2", Map.of("k2", key(2)), blindKey(9)));

        assertThatThrownBy(() -> onlyNew.decrypt(PHONE_OF_A, oldCiphertext)).isInstanceOf(FieldProtectionException.class);
    }

    // ---------------------------------------------------------------- índice cego

    @Test
    void givenTheSameValueAndContext_whenComputingTheBlindIndex_thenItIsDeterministicHex() {
        TinkFieldProtector protector = protector();

        String first = protector.blindIndex(PHONE_OF_A, PHONE);

        assertThat(first).isEqualTo(protector.blindIndex(PHONE_OF_A, PHONE)).matches("[0-9a-f]{64}");
    }

    @Test
    void givenDifferentValuesCompaniesOrFields_whenComputingTheBlindIndex_thenTheyDiffer() {
        TinkFieldProtector protector = protector();
        String base = protector.blindIndex(PHONE_OF_A, PHONE);

        assertThat(protector.blindIndex(PHONE_OF_A, "+5511987654322")).isNotEqualTo(base);
        assertThat(protector.blindIndex(new FieldContext(COMPANY_B, "contact.phone"), PHONE)).isNotEqualTo(base);
        assertThat(protector.blindIndex(new FieldContext(COMPANY_A, "vehicle.plate"), PHONE)).isNotEqualTo(base);
    }

    @Test
    void givenTheBlindIndex_whenLookingAtIt_thenTheValueIsNotInIt() {
        assertThat(protector().blindIndex(PHONE_OF_A, PHONE)).doesNotContain("987654321");
    }

    @Test
    void givenSeparateKeys_whenChangingOne_thenTheOtherDoesNotAffectTheBlindIndexOrTheCipher() {
        String base = protector().blindIndex(PHONE_OF_A, PHONE);
        TinkFieldProtector otherEncryptionKey = new TinkFieldProtector(settings("k1", Map.of("k1", key(5)), blindKey(9)));
        TinkFieldProtector otherBlindKey = new TinkFieldProtector(settings("k1", Map.of("k1", key(1)), blindKey(8)));

        assertThat(otherEncryptionKey.blindIndex(PHONE_OF_A, PHONE)).isEqualTo(base);
        assertThat(otherBlindKey.blindIndex(PHONE_OF_A, PHONE)).isNotEqualTo(base);
        byte[] ciphertext = protector().encrypt(PHONE_OF_A, PHONE);
        assertThat(otherBlindKey.decrypt(PHONE_OF_A, ciphertext)).isEqualTo(PHONE);
    }

    // ---------------------------------------------------------------- configuração

    @Test
    void givenInvalidSettings_whenCreating_thenTheyAreRefused() {
        assertThatThrownBy(() -> settings("k1", Map.of("k1", new byte[16]), blindKey(9))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("256 bits");
        assertThatThrownBy(() -> settings("k1", Map.of("k1", new byte[33]), blindKey(9))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> settings("k9", Map.of("k1", key(1)), blindKey(9))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("active");
        assertThatThrownBy(() -> settings("k1", Map.of("k1", key(1)), new byte[16])).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blind");
    }

    @Test
    void givenSettings_whenPrinting_thenNoKeyMaterialAppears() {
        assertThat(settings("k1", Map.of("k1", key(1)), blindKey(9))).hasToString("FieldEncryptionSettings[redacted]");
    }

    @Test
    void givenTheSameKeyUnderTwoNames_whenCreating_thenItIsRefusedBecauseTheIdsWouldCollideOrRepeat() {
        assertThatThrownBy(() -> new TinkFieldProtector(settings("k1", Map.of("k1", key(1), "k2", key(1)), blindKey(9))))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
