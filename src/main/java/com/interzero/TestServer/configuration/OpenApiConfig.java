package com.interzero.TestServer.configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI (Swagger) documentation settings.
 * <p>
 * Declares HTTP Basic authentication and applies it to every endpoint. This adds the "Authorize" button to Swagger UI:
 * enter a username and password once and "Try it out" sends them with every request. Logging in again as another user
 * switches roles, e.g. to see that a reader cannot write.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Pet Management API",
                version = "v1",
                description = "Manage owners and their pets. Every endpoint requires HTTP Basic authentication: "
                        + "click \"Authorize\" and log in as a user from application.properties. "
                        + "READER can read, WRITER can create, update and delete, ADMIN can do both."),
        security = @SecurityRequirement(name = OpenApiConfig.BASIC_AUTH))
@SecurityScheme(
        name = OpenApiConfig.BASIC_AUTH,
        type = SecuritySchemeType.HTTP,
        scheme = "basic",
        description = "Username and password of one of the users configured in application.properties.")
public class OpenApiConfig {

    /**
     * The name of the security scheme in the OpenAPI document.
     */
    static final String BASIC_AUTH = "basicAuth";
}
