package br.com.erpcore.adapter.web.ratelimit;

import br.com.erpcore.adapter.web.WebProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Os limites das rotas públicas. A chave por e-mail é o hash do e-mail: assim nem a memória do limitador guarda
 * o endereço. Atrás de proxy reverso o IP visto é o do proxy.
 * TODO(decisão): definir como confiar em X-Forwarded-For quando houver proxy reverso na frente.
 */
@Component
public final class RateLimits {

    private static final int MAX_TRACKED_KEYS = 50_000;

    private final WebProperties.RateLimit limits;
    private final InMemoryRateLimiter limiter;

    public RateLimits(WebProperties properties, Clock clock) {
        this.limits = properties.rateLimit();
        this.limiter = new InMemoryRateLimiter(clock, MAX_TRACKED_KEYS);
    }

    public void checkRegister(String clientAddress) {
        require("register-ip:" + clientAddress, limits.registerPerIp(), limits.registerWindow());
    }

    public void checkLogin(String clientAddress, String email) {
        require("login-ip:" + clientAddress, limits.loginPerIp(), limits.loginWindow());
        require("login-email:" + sha256(email == null ? "" : email.trim().toLowerCase(Locale.ROOT)),
                limits.loginPerEmail(), limits.loginWindow());
    }

    public void checkRefresh(String clientAddress) {
        require("refresh-ip:" + clientAddress, limits.refreshPerIp(), limits.refreshWindow());
    }

    /**
     * O link público de orçamento: um limite por endereço (contra quem tenta adivinhar tokens) e um por token (contra
     * quem martela um link só). A chave por token é o hash dele: a memória do limitador não guarda a credencial.
     */
    public void checkPublicLink(String clientAddress, String token) {
        require("link-ip:" + clientAddress, limits.publicLinkPerIp(), limits.publicLinkWindow());
        require("link-token:" + sha256(token == null ? "" : token), limits.publicLinkPerToken(), limits.publicLinkWindow());
    }

    private void require(String key, int limit, Duration window) {
        RateLimitDecision decision = limiter.tryAcquire(key, limit, window);
        if (!decision.allowed()) {
            throw new TooManyRequestsException(decision.retryAfter());
        }
    }

    private static String sha256(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
