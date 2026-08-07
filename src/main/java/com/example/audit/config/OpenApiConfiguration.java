package com.example.audit.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {
    @Bean
    OpenAPI auditLogOpenApi() {
        return new OpenAPI().info(new Info().title("Tamper-Evident Audit Log Service")
                .version("1.0.0").description("Append-only audit events with SHA-256 chain verification."));
    }
}
