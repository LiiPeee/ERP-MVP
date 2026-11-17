package br.com.erpcore.adapter.infra.security;

import br.com.erpcore.application.identity.port.out.AccessTokenIssuer;
import br.com.erpcore.application.identity.port.out.AccessTokenSubject;
import br.com.erpcore.application.identity.port.out.IssuedAccessToken;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

public final class JwtAccessTokenIssuer implements AccessTokenIssuer {

    private final JwtSettings settings;
    private final Clock clock;
    private final NimbusJwtEncoder encoder;

    public JwtAccessTokenIssuer(JwtSettings settings, Clock clock) {
        this.settings = settings;
        this.clock = clock;
        List<JWK> keys = settings.keys().entrySet().stream()
                .<JWK>map(entry -> new OctetSequenceKey.Builder(entry.getValue()).keyID(entry.getKey())
                        .algorithm(com.nimbusds.jose.JWSAlgorithm.HS256).build())
                .toList();
        this.encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(keys)));
    }

    @Override
    public IssuedAccessToken issue(AccessTokenSubject subject) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(settings.accessTokenLifetime());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(settings.issuer())
                .audience(List.of(settings.audience()))
                .subject(subject.userId().toString())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .claim(JwtDecoderFactory.COMPANY_CLAIM, subject.companyId().toString())
                .claim(JwtDecoderFactory.ROLE_CLAIM, subject.role())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).keyId(settings.activeKeyId()).type("JWT").build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedAccessToken(token, expiresAt);
    }
}
