package br.com.erpcore.bootstrap;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("erp.security.session")
record SessionProperties(
        @DefaultValue("7d") Duration refreshTokenLifetime,
        @DefaultValue("30d") Duration maxAge) {
}
