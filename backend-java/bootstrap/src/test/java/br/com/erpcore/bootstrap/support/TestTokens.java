package br.com.erpcore.bootstrap.support;

import br.com.erpcore.adapter.infra.security.JwtAccessTokenIssuer;
import br.com.erpcore.adapter.infra.security.JwtSettings;
import br.com.erpcore.application.identity.port.out.AccessTokenSubject;
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
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/** Fabrica tokens de acesso válidos e inválidos de propósito, para os testes de segurança. */
public final class TestTokens {

    private static final String ISSUER = "erp-core";
    private static final String AUDIENCE = "erp-core-api";
    private static final byte[] OTHER_SECRET = "segredo-do-atacante-sintetico-com-32-bytes".getBytes();

    private TestTokens() {
    }

    private static JwtSettings settings(byte[] secret) {
        return new JwtSettings(ISSUER, AUDIENCE, Duration.ofMinutes(15), ApplicationHttpTest.ACTIVE_KEY_ID,
                Map.of(ApplicationHttpTest.ACTIVE_KEY_ID, secret));
    }

    private static String issue(JwtSettings settings, Clock clock, UUID user, UUID company) {
        return new JwtAccessTokenIssuer(settings, clock).issue(new AccessTokenSubject(user, company, "OWNER")).value();
    }

    /** Assinado pela chave verdadeira: o servidor o aceita (se o usuário for mesmo da empresa). */
    public static String valid(UUID user, UUID company) {
        return issue(settings(ApplicationHttpTest.ACTIVE_SECRET), Clock.systemUTC(), user, company);
    }

    public static String expired(UUID user, UUID company) {
        Clock anHourAgo = Clock.fixed(Instant.now().minus(Duration.ofHours(1)), Clock.systemUTC().getZone());
        return issue(settings(ApplicationHttpTest.ACTIVE_SECRET), anHourAgo, user, company);
    }

    public static String signedWithAnotherKey(UUID user, UUID company) {
        return issue(settings(OTHER_SECRET), Clock.systemUTC(), user, company);
    }

    public static String unsigned(UUID user, UUID company) {
        return new PlainJWT(new PlainHeader(), claims(user, company).build()).serialize();
    }

    public static String withoutCompany(UUID user) {
        return sign(ApplicationHttpTest.ACTIVE_KEY_ID, claims(user, UUID.randomUUID()).claim("cid", null).build());
    }

    public static String withUnknownKeyId(UUID user, UUID company) {
        return sign("k-desconhecida", claims(user, company).build());
    }

    private static JWTClaimsSet.Builder claims(UUID user, UUID company) {
        Instant now = Instant.now();
        return new JWTClaimsSet.Builder().issuer(ISSUER).audience(AUDIENCE).subject(user.toString())
                .claim("cid", company.toString()).claim("role", "OWNER")
                .issueTime(Date.from(now)).expirationTime(Date.from(now.plus(Duration.ofMinutes(15))));
    }

    private static String sign(String keyId, JWTClaimsSet claims) {
        try {
            SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.HS256).keyID(keyId).build(), claims);
            jwt.sign(new MACSigner(ApplicationHttpTest.ACTIVE_SECRET));
            return jwt.serialize();
        } catch (com.nimbusds.jose.JOSEException failure) {
            throw new IllegalStateException(failure);
        }
    }
}
