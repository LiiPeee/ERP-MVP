package br.com.erpcore.bootstrap.support;

import br.com.erpcore.adapter.persistence.support.PostgresTestDatabase;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.test.context.ContextConfiguration;

/**
 * A aplicação inteira num servidor real, com o PostgreSQL de teste, o papel real da aplicação e chaves de
 * assinatura sintéticas. Os limites de requisição são altos para não atrapalhar; os testes de limite usam os seus.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = ApplicationHttpTest.Initializer.class)
public @interface ApplicationHttpTest {

    String ACTIVE_KEY_ID = "k1";
    byte[] ACTIVE_SECRET = "segredo-sintetico-de-teste-com-mais-de-32-bytes".getBytes();

    class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext context) {
            List<String> properties = new ArrayList<>(PostgresTestDatabase.applicationRoleProperties());
            properties.add("erp.security.jwt.active-key-id=" + ACTIVE_KEY_ID);
            properties.add("erp.security.jwt.keys." + ACTIVE_KEY_ID + "=" + Base64.getEncoder().encodeToString(ACTIVE_SECRET));
            properties.add("erp.security.field.active-key-id=" + ACTIVE_KEY_ID);
            properties.add("erp.security.field.keys." + ACTIVE_KEY_ID + "="
                    + Base64.getEncoder().encodeToString("chave-sintetica-de-teste-aes-256".getBytes()));
            properties.add("erp.security.field.blind-index-key="
                    + Base64.getEncoder().encodeToString("chave-sintetica-do-indice-cego-de-teste".getBytes()));
            TestPropertyValues.of(properties.toArray(String[]::new)).applyTo(context);
            // Limites altos por padrão, com a menor precedência: um @TestPropertySource consegue baixá-los.
            context.getEnvironment().getPropertySources().addLast(new MapPropertySource("high-rate-limits", Map.of(
                    "erp.web.rate-limit.login-per-ip", "1000",
                    "erp.web.rate-limit.login-per-email", "1000",
                    "erp.web.rate-limit.register-per-ip", "1000",
                    "erp.web.rate-limit.refresh-per-ip", "1000")));
        }
    }
}
