package br.com.erpcore.adapter.web.frontend;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * As rotas da tela (Angular) caem no index.html, para recarregar a página em /login ou /me funcionar. Os arquivos
 * vêm de {@code spring.web.resources.static-locations}; sem eles (nos testes, por exemplo) a resposta é 404.
 */
@Configuration(proxyBeanMethods = false)
class FrontendRoutesConfiguration implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        for (String route : new String[] {"/login", "/register", "/me", "/o/{token}", "/fila", "/oficina",
                "/orcamento/novo", "/orcamento/{id}"}) {
            registry.addViewController(route).setViewName("forward:/index.html");
        }
    }
}
