package br.com.erpcore.bootstrap;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Criptografia de campo. As chaves de cifrar (AES-256, exatamente 32 bytes em Base64) e a chave do índice cego (HMAC,
 * no mínimo 32 bytes em Base64) vêm só do ambiente: sem elas a aplicação não sobe. Para trocar a chave de cifrar,
 * configure a nova ao lado da antiga, aponte a ativa para a nova e mantenha a antiga até regravar o que foi cifrado com
 * ela. O índice cego não tem rotação: trocá-lo exige reindexar.
 */
@ConfigurationProperties("erp.security.field")
record FieldEncryptionProperties(String activeKeyId, @DefaultValue Map<String, String> keys, String blindIndexKey) {

    @Override
    public String toString() {
        return "FieldEncryptionProperties[redacted]";
    }
}
