package com.gryffindor.excalibur.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info =
        @Info(
            title = "Sujalam Agro Foods API",
            version = "1.0",
            description =
                "RESTful Backend API for Sujalam Agro Foods e-commerce platform. Supports public catalog browsing, customer order placement, Firebase authentication, and administrative stock & order management.",
            contact =
                @Contact(name = "Sujalam Agro Foods Support", email = "support@sujalamagro.com")),
    security = @SecurityRequirement(name = "BearerAuth"))
@SecurityScheme(
    name = "BearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "Enter Firebase ID Token (JWT) to authenticate requests.")
public class OpenApiConfig {}
