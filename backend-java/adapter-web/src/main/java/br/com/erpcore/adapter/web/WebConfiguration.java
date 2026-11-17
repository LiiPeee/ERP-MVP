package br.com.erpcore.adapter.web;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/** Liga a borda HTTP: controllers, filtros, segurança e tratamento de erros. O bootstrap importa esta classe. */
@Configuration(proxyBeanMethods = false)
@ComponentScan(basePackageClasses = WebConfiguration.class)
@EnableConfigurationProperties(WebProperties.class)
public class WebConfiguration {
}
