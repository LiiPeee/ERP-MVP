package br.com.erpcore.adapter.web.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** O contrato da API. O cliente do frontend é gerado a partir dele. */
@Configuration(proxyBeanMethods = false)
class OpenApiConfiguration {

    @Bean
    OpenAPI erpCoreOpenApi() {
        return new OpenAPI()
                .info(new Info().title("ERP Core API").version("0.1.0")
                        .description("Fase 0: cadastro, entrada, renovação de sessão, saída e contexto do usuário."))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }
}
