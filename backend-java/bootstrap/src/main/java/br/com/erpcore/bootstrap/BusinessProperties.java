package br.com.erpcore.bootstrap;

import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * O fuso da oficina decide o fim do dia da validade do orçamento (padrão America/Sao_Paulo) e o endereço público é a
 * base do link que o cliente abre. O domínio definitivo ainda é decisão em aberto.
 */
@ConfigurationProperties("erp.business")
record BusinessProperties(@DefaultValue("America/Sao_Paulo") ZoneId zone,
                          @DefaultValue("http://localhost:8080") String publicBaseUrl) {
}
