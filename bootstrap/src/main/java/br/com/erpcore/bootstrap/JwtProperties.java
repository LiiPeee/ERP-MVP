package br.com.erpcore.bootstrap;

import java.time.Duration;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Token de acesso. As chaves (HMAC-SHA256, em Base64) e a chave ativa vêm só do ambiente: sem elas a aplicação
 * não sobe. Para trocar a chave, configure a nova ao lado da antiga, aponte a ativa para a nova e, depois que os
 * tokens antigos expirarem, remova a antiga.
 */
@ConfigurationProperties("erp.security.jwt")
record JwtProperties(
        @DefaultValue("erp-core") String issuer,
        @DefaultValue("erp-core-api") String audience,
        @DefaultValue("15m") Duration accessTokenLifetime,
        String activeKeyId,
        @DefaultValue Map<String, String> keys) {
}
