package br.com.erpcore.adapter.web.ratelimit;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Janela fixa por chave, na memória desta instância (zera ao reiniciar e não é compartilhada entre instâncias).
 * A tabela de chaves tem teto: cheia de entradas vivas, recusa chaves novas em vez de crescer sem limite (um
 * atacante com milhões de e-mails não esgota a memória; o custo é recusar quem chega durante o ataque).
 */
public final class InMemoryRateLimiter {

    private record Window(Instant startedAt, Duration length, int count) {
        Instant endsAt() {
            return startedAt.plus(length);
        }
    }

    private final Clock clock;
    private final int maxKeys;
    private final Map<String, Window> windows = new HashMap<>();

    public InMemoryRateLimiter(Clock clock, int maxKeys) {
        this.clock = clock;
        this.maxKeys = maxKeys;
    }

    public synchronized RateLimitDecision tryAcquire(String key, int limit, Duration window) {
        Instant now = clock.instant();
        Window current = windows.get(key);
        if (current == null || !now.isBefore(current.endsAt())) {
            if (current == null && windows.size() >= maxKeys) {
                dropExpired(now);
                if (windows.size() >= maxKeys) {
                    return RateLimitDecision.refused(window);
                }
            }
            windows.put(key, new Window(now, window, 1));
            return RateLimitDecision.permitted();
        }
        if (current.count() >= limit) {
            return RateLimitDecision.refused(Duration.between(now, current.endsAt()));
        }
        windows.put(key, new Window(current.startedAt(), current.length(), current.count() + 1));
        return RateLimitDecision.permitted();
    }

    synchronized int trackedKeys() {
        return windows.size();
    }

    private void dropExpired(Instant now) {
        windows.values().removeIf(window -> !now.isBefore(window.endsAt()));
    }
}
