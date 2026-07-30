package com.angemau.medisalud.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI medisaludOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("MediSalud API")
                .description("Sistema de agendamiento de citas médicas")
                .version("1.0"));
    }
}
