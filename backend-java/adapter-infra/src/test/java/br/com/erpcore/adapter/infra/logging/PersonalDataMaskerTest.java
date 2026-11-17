package br.com.erpcore.adapter.infra.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PersonalDataMaskerTest {

    @Test
    void givenAnEmailInAMessage_whenMasking_thenItIsReplaced() {
        String masked = PersonalDataMasker.mask("login de ana.ficticia+teste@exemplo.com.br falhou");

        assertThat(masked).isEqualTo("login de [email] falhou");
    }

    @ParameterizedTest
    @ValueSource(strings = {"123.456.789-09", "12345678909"})
    void givenACpfWithOrWithoutPunctuation_whenMasking_thenItIsReplaced(String cpf) {
        assertThat(PersonalDataMasker.mask("cpf " + cpf + " informado")).isEqualTo("cpf [cpf] informado");
    }

    @ParameterizedTest
    @ValueSource(strings = {"12.345.678/0001-95", "12345678000195"})
    void givenACnpjWithOrWithoutPunctuation_whenMasking_thenItIsReplaced(String cnpj) {
        assertThat(PersonalDataMasker.mask("cnpj " + cnpj + " informado")).isEqualTo("cnpj [cnpj] informado");
    }

    @Test
    void givenAJwtAndABearerHeader_whenMasking_thenTheTokenIsReplaced() {
        String jwt = "eyJhbGciOiJIUzI1NiIsImtpZCI6ImsxIn0.eyJzdWIiOiIxMjMifQ.c2lnbmF0dXJlLXNpbnRldGljYQ";

        assertThat(PersonalDataMasker.mask("Authorization: Bearer " + jwt)).isEqualTo("Authorization: Bearer [token]");
    }

    @Test
    void givenARefreshTokenOrAHash_whenMasking_thenItIsReplaced() {
        String refresh = "Zm9vYmFyLXRva2VuLXNpbnRldGljby1kZS0yNTYtYml0cy0wMTIzNDU";
        String hash = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";

        assertThat(PersonalDataMasker.mask("cookie=" + refresh + " hash=" + hash)).isEqualTo("cookie=[token] hash=[token]");
    }

    @Test
    void givenSeveralSensitiveValues_whenMasking_thenAllAreReplaced() {
        String masked = PersonalDataMasker.mask("a@b.test 123.456.789-09 12.345.678/0001-95 a@b.test");

        assertThat(masked).isEqualTo("[email] [cpf] [cnpj] [email]");
    }

    @Test
    void givenAlreadyMaskedText_whenMaskingAgain_thenNothingChanges() {
        String once = PersonalDataMasker.mask("a@b.test 123.456.789-09");

        assertThat(PersonalDataMasker.mask(once)).isEqualTo(once);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "correlation_id=3f2b8c1e-6a4d-4e0b-9d5a-7c1f2e3a4b5c",
            "id 2848f2cd-ba32-4fa5-9e6b-47196187436d",
            "id 00000000-0000-0000-0000-000000000011",
            "id 12345678-1234-1234-1234-12345678901a",
            "2026-07-01T12:00:00.123456Z",
            "HTTP 401 em POST /api/auth/login em 12 ms",
            "usuário 42 acessou a empresa 7",
            "epoch 1782907200123",
            "br.com.erpcore.adapter.web.auth.AuthController"})
    void givenOrdinaryDiagnosticText_whenMasking_thenItIsLeftAlone(String text) {
        assertThat(PersonalDataMasker.mask(text)).isEqualTo(text);
    }

    @ParameterizedTest
    @ValueSource(strings = {"+5511955554444", "+551132224444", "+55 (11) 95555-4444", "(11) 95555-4444"})
    void givenABrazilianPhoneInAnyFormat_whenMasking_thenItIsReplaced(String phone) {
        assertThat(PersonalDataMasker.mask("cliente " + phone + " cadastrado")).isEqualTo("cliente [phone] cadastrado");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ABC1D23", "ABC-1234", "ABC1234"})
    void givenALicensePlateOldOrMercosul_whenMasking_thenItIsReplaced(String plate) {
        assertThat(PersonalDataMasker.mask("placa " + plate + " informada")).isEqualTo("placa [plate] informada");
    }

    @Test
    void givenTheQuoteLinkTokenInAPath_whenMasking_thenItIsReplaced() {
        String token = "k3J9x_Qw-7ZpL0aVbN2mC8dR5tYhGfE1uIoS4xWzAqM";

        assertThat(PersonalDataMasker.mask("GET /o/" + token + " 200")).isEqualTo("GET /o/[token] 200");
        assertThat(PersonalDataMasker.mask("path=/api/public/quotes/" + token + "/approve"))
                .isEqualTo("path=/api/public/quotes/[token]/approve");
    }

    @ParameterizedTest
    @ValueSource(strings = {"path=/api/quotes/2848f2cd-ba32-4fa5-9e6b-47196187436d/confirm", "status=SENT total=131.00",
            "quote APPROVED em 2026-07-01", "ABC e 1234 sozinhos", "plano ABCD1234"})
    void givenQuoteDiagnosticText_whenMasking_thenItIsLeftAlone(String text) {
        assertThat(PersonalDataMasker.mask(text)).isEqualTo(text);
    }

    @Test
    void givenNull_whenMasking_thenNullComesBack() {
        assertThat(PersonalDataMasker.mask(null)).isNull();
    }
}
