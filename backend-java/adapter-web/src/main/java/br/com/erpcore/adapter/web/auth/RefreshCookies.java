package br.com.erpcore.adapter.web.auth;

import br.com.erpcore.adapter.web.WebProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * O refresh token vive só neste cookie: inacessível ao JavaScript (HttpOnly), só por HTTPS (Secure), nunca em
 * requisição de outro site (SameSite=Strict), restrito às rotas de autenticação e sem Domain (só este host).
 */
@Component
public final class RefreshCookies {

    public static final String NAME = "refresh_token";
    static final String PATH = "/api/auth";

    private final WebProperties properties;
    private final Clock clock;

    public RefreshCookies(WebProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public ResponseCookie issue(String value, Instant expiresAt) {
        return base(value).maxAge(Duration.between(clock.instant(), expiresAt)).build();
    }

    public ResponseCookie clear() {
        return base("").maxAge(Duration.ZERO).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(properties.cookieSecure())
                .sameSite("Strict")
                .path(PATH);
    }
}
