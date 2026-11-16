package br.com.erpcore.adapter.persistence.support;

import br.com.erpcore.adapter.persistence.PersistenceTestApplication;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;

/** Contexto que conecta como o papel real da aplicação: sem posse, sem superusuário, sujeito a RLS. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(classes = PersistenceTestApplication.class)
@ContextConfiguration(initializers = ApplicationRolePostgresTest.Initializer.class)
public @interface ApplicationRolePostgresTest {

    class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext context) {
            TestPropertyValues.of(PostgresTestDatabase.applicationRoleProperties().toArray(String[]::new))
                    .applyTo(context);
        }
    }
}
