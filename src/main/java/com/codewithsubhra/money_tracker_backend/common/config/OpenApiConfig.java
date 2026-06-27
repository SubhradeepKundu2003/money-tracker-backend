package com.codewithsubhra.money_tracker_backend.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger metadata. Declares the opaque session token as an HTTP bearer
 * scheme so the "Authorize" button in Swagger UI sends
 * {@code Authorization: Bearer <sessionToken>} on protected endpoints.
 */
@Configuration
public class OpenApiConfig {

    private static final String SESSION_SCHEME = "sessionToken";

    @Bean
    public OpenAPI moneyTrackerOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Money Tracker API")
                        .version("v1")
                        .description("""
                                Personal money-management API.

                                **Auth:** call `/api/auth/login` or `/api/auth/register`, copy the
                                `sessionToken` from the response, click **Authorize**, and paste it.
                                When it expires, use `/api/auth/refresh` with your `refreshToken`.""")
                        .license(new License().name("Proprietary")))
                .addSecurityItem(new SecurityRequirement().addList(SESSION_SCHEME))
                .components(new Components().addSecuritySchemes(SESSION_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .description("Opaque session token returned by login/register/refresh")));
    }
}
