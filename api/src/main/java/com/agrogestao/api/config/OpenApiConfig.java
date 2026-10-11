package com.agrogestao.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Esquema Bearer no Swagger: habilita o botão Authorize e envia o token nas chamadas. */
@Configuration
public class OpenApiConfig {

  static final String ESQUEMA_BEARER = "bearerAuth";

  @Bean
  public OpenAPI openApi() {
    return new OpenAPI()
        .info(new Info().title("AgroGestão API").version("v1"))
        .components(
            new Components()
                .addSecuritySchemes(
                    ESQUEMA_BEARER,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
        .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_BEARER));
  }
}
