package br.com.erpcore.adapter.infra.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class Argon2PasswordHasherTest {

    private static final String PASSWORD = "senha-sintetica-12345";

    private final Argon2PasswordHasher hasher = new Argon2PasswordHasher();

    @Test
    void givenAPassword_whenHashing_thenTheResultIsArgon2idWithTheConfiguredCost() {
        String hash = hasher.hash(PASSWORD);

        assertThat(hash).startsWith("$argon2id$v=19$m=19456,t=2,p=1$");
        assertThat(hash).doesNotContain(PASSWORD);
    }

    @Test
    void givenTheSamePasswordTwice_whenHashing_thenTheHashesDifferBecauseOfTheSalt() {
        assertThat(hasher.hash(PASSWORD)).isNotEqualTo(hasher.hash(PASSWORD));
    }

    @Test
    void givenTheCorrectPassword_whenVerifying_thenItMatches() {
        assertThat(hasher.matches(PASSWORD, hasher.hash(PASSWORD))).isTrue();
    }

    @Test
    void givenAWrongPassword_whenVerifying_thenItDoesNotMatch() {
        assertThat(hasher.matches("outra-senha-sintetica", hasher.hash(PASSWORD))).isFalse();
    }

    @Test
    void givenAStoredValueThatIsNotAnArgon2Hash_whenVerifying_thenItDoesNotMatchInsteadOfFailing() {
        assertThat(hasher.matches(PASSWORD, "isto-nao-e-um-hash")).isFalse();
        assertThat(hasher.matches(PASSWORD, "$argon2id$quebrado")).isFalse();
    }

    @Test
    void givenNoHashToCompare_whenSpendingVerificationTime_thenOneRealVerificationIsPerformed() {
        List<String> verified = new ArrayList<>();
        PasswordEncoder recording = new PasswordEncoder() {
            @Override
            public String encode(CharSequence raw) {
                return "placeholder-hash";
            }

            @Override
            public boolean matches(CharSequence raw, String encoded) {
                verified.add(encoded);
                return false;
            }
        };
        Argon2PasswordHasher withRecording = new Argon2PasswordHasher(recording);

        withRecording.spendVerificationTime(PASSWORD);

        assertThat(verified).containsExactly("placeholder-hash");
    }

    @Test
    void givenTheDefaultHasher_whenSpendingVerificationTime_thenItReturnsWithoutFailing() {
        hasher.spendVerificationTime(PASSWORD);
        hasher.spendVerificationTime("");
    }
}
