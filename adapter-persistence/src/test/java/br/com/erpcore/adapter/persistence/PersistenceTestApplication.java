package br.com.erpcore.adapter.persistence;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Import;

/** Aplicação mínima só para testar este módulo: auto-configuração do Spring mais a configuração de persistência. */
@SpringBootConfiguration
@EnableAutoConfiguration
@Import({PersistenceConfiguration.class, PersistenceTestBeans.class})
public class PersistenceTestApplication {
}
