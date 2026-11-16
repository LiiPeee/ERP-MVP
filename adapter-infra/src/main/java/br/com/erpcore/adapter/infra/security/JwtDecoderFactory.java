package br.com.erpcore.adapter.infra.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Valida o token de acesso: só HS256, chave escolhida pelo {@code kid}, emissor, audiência, validade e as
 * afirmações de empresa e papel. Qualquer coisa fora disso é rejeitada, inclusive tokens sem assinatura.
 */
public final class JwtDecoderFactory {

    public static final String COMPANY_CLAIM = "cid";
    public static final String ROLE_CLAIM = "role";

    private static final List<String> KNOWN_ROLES = List.of("OWNER");

    private JwtDecoderFactory() {
    }

    public static JwtDecoder create(JwtSettings settings, Clock clock) {
        List<JWK> keys = settings.keys().entrySet().stream()
                .<JWK>map(entry -> new OctetSequenceKey.Builder(entry.getValue()).keyID(entry.getKey())
                        .algorithm(JWSAlgorithm.HS256).build())
                .toList();
        DefaultJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
        processor.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.HS256,
                new ImmutableJWKSet<>(new JWKSet(keys))));
        processor.setJWTClaimsSetVerifier((claims, context) -> { });

        JwtTimestampValidator timestamps = new JwtTimestampValidator(Duration.ZERO);
        timestamps.setClock(clock);
        NimbusJwtDecoder decoder = new NimbusJwtDecoder(processor);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(timestamps,
                new JwtIssuerValidator(settings.issuer()), audience(settings.audience()), tenantClaims()));
        return decoder;
    }

    private static OAuth2TokenValidator<Jwt> audience(String expected) {
        return jwt -> jwt.getAudience() != null && jwt.getAudience().contains(expected)
                ? OAuth2TokenValidatorResult.success() : failure("invalid audience");
    }

    private static OAuth2TokenValidator<Jwt> tenantClaims() {
        return jwt -> isUuid(jwt.getClaimAsString(JwtClaimNames.SUB)) && isUuid(jwt.getClaimAsString(COMPANY_CLAIM))
                && isKnownRole(jwt.getClaimAsString(ROLE_CLAIM))
                ? OAuth2TokenValidatorResult.success() : failure("invalid tenant claims");
    }

    private static boolean isKnownRole(String role) {
        return role != null && KNOWN_ROLES.contains(role);
    }

    private static boolean isUuid(String value) {
        if (value == null) {
            return false;
        }
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException notAUuid) {
            return false;
        }
    }

    private static OAuth2TokenValidatorResult failure(String description) {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", description, null));
    }
}
