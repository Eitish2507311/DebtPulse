package com.debtpulse.notification.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger / OpenAPI documentation for notification-service with a JWT bearer scheme so the
 * "Authorize" button in Swagger UI works. Reachable directly at
 * http://localhost:8088/swagger-ui.html and through the gateway aggregation.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI notificationServiceOpenApi() {
        final String scheme = "bearerAuth";
        return new OpenAPI()
                .servers(List.of(new Server().url("/").description("Relative to the origin serving this doc -- through the API Gateway when accessed there")))
                .info(new Info()
                        .title("DebtPulse — Notification Service API")
                        .description("Notifications & Alerts: per-user in-app notifications")
                        .version("1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList(scheme))
                .components(new Components().addSecuritySchemes(scheme,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
