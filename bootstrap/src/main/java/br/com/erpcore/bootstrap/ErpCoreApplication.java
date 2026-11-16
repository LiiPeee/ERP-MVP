package br.com.erpcore.bootstrap;

import br.com.erpcore.adapter.persistence.PersistenceConfiguration;
import br.com.erpcore.adapter.web.WebConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({PersistenceConfiguration.class, WebConfiguration.class})
public class ErpCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(ErpCoreApplication.class, args);
    }
}
