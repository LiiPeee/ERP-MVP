package br.com.erpcore.adapter.web;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuração da borda HTTP. Os padrões são os mais restritivos: cookie só em HTTPS, nenhuma origem externa
 * liberada no CORS e limites baixos nas rotas públicas.
 */
@ConfigurationProperties("erp.web")
public record WebProperties(
        @DefaultValue("true") boolean cookieSecure,
        @DefaultValue List<String> allowedOrigins,
        @DefaultValue RateLimit rateLimit) {

    public record RateLimit(
            @DefaultValue("10") int loginPerIp,
            @DefaultValue("5") int loginPerEmail,
            @DefaultValue("1m") Duration loginWindow,
            @DefaultValue("5") int registerPerIp,
            @DefaultValue("1h") Duration registerWindow,
            @DefaultValue("30") int refreshPerIp,
            @DefaultValue("1m") Duration refreshWindow,
            @DefaultValue("60") int publicLinkPerIp,
            @DefaultValue("20") int publicLinkPerToken,
            @DefaultValue("1m") Duration publicLinkWindow) {
    }
}
