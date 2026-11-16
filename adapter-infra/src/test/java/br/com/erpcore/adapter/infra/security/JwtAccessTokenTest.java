package br.com.erpcore.adapter.infra.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.erpcore.application.identity.port.out.AccessTokenSubject;
import br.com.erpcore.application.identity.port.out.IssuedAccessToken;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.PlainHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

class JwtAccessTokenTest {

    private static final Instant NOW = Instant.parse("2026-07-01T12:00:00Z");
    private static final String ISSUER = "erp-core";
    private static final String AUDIENCE = "erp-core-api";
    private static final String KEY_A = "k1";
    private static final String KEY_B = "k2";
    private static final byte[] SECRET_A = "segredo-sintetico-a-com-32-bytes!".getBytes();
    private static final byte[] SECRET_B = "segredo-sintetico-b-com-32-bytes!".getBytes();
    private static final UUID USER = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID COMPANY = UUID.fromString("00000000-0000-0000-0000-0000000000c1");

    private final MutableClock clock = new MutableClock(NOW);

    private JwtSettings settings(String activeKey, Map<String, byte[]> keys) {
        return new JwtSettings(ISSUER, AUDIENCE, Duration.ofMinutes(15), activeKey, keys);
    }

    private JwtSettings settingsWithKeyA() {
        return settings(KEY_A, Map.of(KEY_A, SECRET_A));
    }

    private IssuedAccessToken issue(JwtSettings settings) {
        return new JwtAccessTokenIssuer(settings, clock).issue(new AccessTokenSubject(USER, COMPANY, "OWNER"));
    }

    private JwtDecoder decoder(JwtSettings settings) {
        return JwtDecoderFactory.create(settings, clock);
    }

    private String signed(String keyId, byte[] secret, JWSAlgorithm algorithm, JWTClaimsSet claims) throws Exception {
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(algorithm).keyID(keyId).build(), claims);
        jwt.sign(new MACSigner(secret));
        return jwt.serialize();
    }

    private JWTClaimsSet.Builder validClaims() {
        return new JWTClaimsSet.Builder().issuer(ISSUER).audience(AUDIENCE).subject(USER.toString())
                .claim("cid", COMPANY.toString()).claim("role", "OWNER")
                .issueTime(Date.from(NOW)).expirationTime(Date.from(NOW.plus(Duration.ofMinutes(15))));
    }

    @Test
    void givenAnIssuedToken_whenDecoding_thenItCarriesTheUserTheCompanyAndTheRole() {
        JwtSettings settings = settingsWithKeyA();

        Jwt jwt = decoder(settings).decode(issue(settings).value());

        assertThat(jwt.getSubject()).isEqualTo(USER.toString());
        assertThat(jwt.<String>getClaim("cid")).isEqualTo(COMPANY.toString());
        assertThat(jwt.<String>getClaim("role")).isEqualTo("OWNER");
        assertThat(jwt.<String>getClaim("iss")).isEqualTo(ISSUER);
        assertThat(jwt.getAudience()).containsExactly(AUDIENCE);
        assertThat(jwt.getId()).isNotBlank();
        assertThat(jwt.getHeaders()).containsEntry("kid", KEY_A).containsEntry("alg", "HS256");
    }

    @Test
    void givenAnIssuedToken_whenReadingItsExpiry_thenItExpiresAfterTheConfiguredLifetime() {
        JwtSettings settings = settingsWithKeyA();

        IssuedAccessToken token = issue(settings);

        assertThat(token.expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(15)));
        assertThat(decoder(settings).decode(token.value()).getExpiresAt()).isEqualTo(token.expiresAt());
    }

    @Test
    void givenTwoTokensForTheSameUser_whenComparingThem_thenEachHasItsOwnId() {
        JwtSettings settings = settingsWithKeyA();

        Jwt first = decoder(settings).decode(issue(settings).value());
        Jwt second = decoder(settings).decode(issue(settings).value());

        assertThat(first.getId()).isNotEqualTo(second.getId());
    }

    @Test
    void givenAnExpiredToken_whenDecoding_thenItIsRejected() {
        JwtSettings settings = settingsWithKeyA();
        String token = issue(settings).value();

        clock.advance(Duration.ofMinutes(15).plusSeconds(1));

        assertThatThrownBy(() -> decoder(settings).decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void givenATokenOneSecondBeforeExpiry_whenDecoding_thenItIsStillAccepted() {
        JwtSettings settings = settingsWithKeyA();
        String token = issue(settings).value();

        clock.advance(Duration.ofMinutes(15).minusSeconds(1));

        assertThat(decoder(settings).decode(token).getSubject()).isEqualTo(USER.toString());
    }

    @Test
    void givenATokenSignedWithAnotherSecret_whenDecoding_thenItIsRejected() throws Exception {
        String forged = signed(KEY_A, "segredo-do-atacante-com-32-bytes!!".getBytes(), JWSAlgorithm.HS256, validClaims().build());

        assertThatThrownBy(() -> decoder(settingsWithKeyA()).decode(forged)).isInstanceOf(JwtException.class);
    }

    @Test
    void givenATamperedPayload_whenDecoding_thenItIsRejected() {
        JwtSettings settings = settingsWithKeyA();
        String[] parts = issue(settings).value().split("\\.");
        String otherPayload = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                ("{\"iss\":\"erp-core\",\"aud\":\"erp-core-api\",\"sub\":\"" + USER + "\",\"cid\":\""
                        + UUID.randomUUID() + "\",\"role\":\"OWNER\",\"exp\":" + NOW.plusSeconds(900).getEpochSecond() + "}").getBytes());

        assertThatThrownBy(() -> decoder(settings).decode(parts[0] + "." + otherPayload + "." + parts[2]))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void givenAnUnsignedToken_whenDecoding_thenItIsRejected() {
        String unsigned = new PlainJWT(new PlainHeader(), validClaims().build()).serialize();

        assertThatThrownBy(() -> decoder(settingsWithKeyA()).decode(unsigned)).isInstanceOf(JwtException.class);
    }

    @Test
    void givenATokenSignedWithAnotherAlgorithmButTheSameKey_whenDecoding_thenItIsRejected() throws Exception {
        byte[] longSecret = "segredo-sintetico-longo-com-64-bytes-para-permitir-o-hs512-no-teste!".getBytes();
        String hs512 = signed(KEY_A, longSecret, JWSAlgorithm.HS512, validClaims().build());

        assertThatThrownBy(() -> decoder(settings(KEY_A, Map.of(KEY_A, longSecret))).decode(hs512))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void givenATokenWithAnUnknownKeyId_whenDecoding_thenItIsRejected() throws Exception {
        String unknownKey = signed("k-desconhecida", SECRET_A, JWSAlgorithm.HS256, validClaims().build());

        assertThatThrownBy(() -> decoder(settingsWithKeyA()).decode(unknownKey)).isInstanceOf(JwtException.class);
    }

    @Test
    void givenATokenFromAnotherIssuerOrAudience_whenDecoding_thenItIsRejected() throws Exception {
        String otherIssuer = signed(KEY_A, SECRET_A, JWSAlgorithm.HS256, validClaims().issuer("outro-emissor").build());
        String otherAudience = signed(KEY_A, SECRET_A, JWSAlgorithm.HS256, validClaims().audience("outra-api").build());

        assertThatThrownBy(() -> decoder(settingsWithKeyA()).decode(otherIssuer)).isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> decoder(settingsWithKeyA()).decode(otherAudience)).isInstanceOf(JwtException.class);
    }

    @Test
    void givenTokensMissingOrMalformingTheTenantClaims_whenDecoding_thenTheyAreRejected() throws Exception {
        List<JWTClaimsSet> invalid = List.of(
                new JWTClaimsSet.Builder(validClaims().build()).claim("cid", null).build(),
                new JWTClaimsSet.Builder(validClaims().build()).claim("cid", "nao-e-uuid").build(),
                new JWTClaimsSet.Builder(validClaims().build()).claim("role", null).build(),
                new JWTClaimsSet.Builder(validClaims().build()).claim("role", "ROLE-INVENTADO").build(),
                new JWTClaimsSet.Builder(validClaims().build()).subject("nao-e-uuid").build(),
                new JWTClaimsSet.Builder(validClaims().build()).subject(null).build());

        for (JWTClaimsSet claims : invalid) {
            String token = signed(KEY_A, SECRET_A, JWSAlgorithm.HS256, claims);
            assertThatThrownBy(() -> decoder(settingsWithKeyA()).decode(token)).as(claims.toString())
                    .isInstanceOf(JwtException.class);
        }
    }

    @Test
    void givenAKeyRotation_whenTheOldKeyIsStillConfigured_thenOldTokensStayValidAndNewOnesUseTheNewKey() {
        JwtSettings before = settings(KEY_A, Map.of(KEY_A, SECRET_A));
        String oldToken = issue(before).value();
        JwtSettings during = settings(KEY_B, Map.of(KEY_A, SECRET_A, KEY_B, SECRET_B));

        String newToken = issue(during).value();

        assertThat(decoder(during).decode(oldToken).getHeaders()).containsEntry("kid", KEY_A);
        assertThat(decoder(during).decode(newToken).getHeaders()).containsEntry("kid", KEY_B);
    }

    @Test
    void givenAKeyRotation_whenTheOldKeyIsRemoved_thenTokensSignedWithItAreRejected() {
        String oldToken = issue(settings(KEY_A, Map.of(KEY_A, SECRET_A))).value();
        JwtSettings after = settings(KEY_B, Map.of(KEY_B, SECRET_B));

        assertThatThrownBy(() -> decoder(after).decode(oldToken)).isInstanceOf(JwtException.class);
    }

    @Test
    void givenAKeyShorterThan256Bits_whenCreatingTheSettings_thenItIsRejected() {
        assertThatThrownBy(() -> settings(KEY_A, Map.of(KEY_A, "curta".getBytes())))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenAnActiveKeyThatIsNotConfigured_whenCreatingTheSettings_thenItIsRejected() {
        assertThatThrownBy(() -> settings("k9", Map.of(KEY_A, SECRET_A))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenTheSettings_whenPrintingThem_thenNoKeyMaterialAppears() {
        assertThat(settingsWithKeyA().toString()).doesNotContain("segredo-sintetico");
    }

    private static final class MutableClock extends Clock {

        private Instant now;

        MutableClock(Instant start) {
            this.now = start;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }
    }
}
