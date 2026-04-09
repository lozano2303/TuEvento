package com.capysoft.tuevento.shared.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI tuEventoOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Tu Evento API")
                        .description("API REST para la plataforma de gestión de eventos y tickets")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Capysoft")
                                .email("contact@capysoft.com"))
                        .license(new License()
                                .name("Private")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Ingresa el JWT token obtenido del endpoint de login")));
    }
}
