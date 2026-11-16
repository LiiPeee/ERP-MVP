package br.com.erpcore.bootstrap;

import br.com.erpcore.adapter.persistence.PersistenceConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(PersistenceConfiguration.class)
public class ErpCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(ErpCoreApplication.class, args);
    }
}
