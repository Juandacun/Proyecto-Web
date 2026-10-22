package com.example.proyectoweb.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Sistema de reservas, préstamos y mantenimiento de recursos - API REST")
                        .version("2.0")
                        .description("API REST para la arquitectura SPA (Angular + Spring Boot) con gestión de recursos, préstamos, reservas e incidentes.")
                        .contact(new Contact().name("Equipo de Desarrollo"))
                        .license(new License().name("Apache 2.0")));
    }
}
