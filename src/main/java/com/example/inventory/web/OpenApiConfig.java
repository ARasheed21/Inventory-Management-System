package com.example.inventory.web;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI apiOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Inventory Management System API")
                        .version("0.1.0")
                        .description("""
                                Backend contract for the Inventory Management System frontend clients.

                                Authentication: obtain a JWT via POST /auth/login or POST /auth/register,
                                then send `Authorization: Bearer <accessToken>` on every protected call.
                                Refresh tokens are exchanged at POST /auth/refresh.

                                Real-time updates are delivered over STOMP over WebSocket at /api/ws
                                (see docs/api-contract/asyncapi-ws.md) - they are not part of this
                                OpenAPI document."""))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
