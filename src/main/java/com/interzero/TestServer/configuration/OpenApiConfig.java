package com.interzero.TestServer.configuration;

import com.interzero.TestServer.controller.AbstractEntityController;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI (Swagger) documentation settings.
 * <p>
 * Declares HTTP Basic authentication and applies it to every endpoint. This adds the "Authorize" button to Swagger UI:
 * enter a username and password once and "Try it out" sends them with every request. Logging in again as another user
 * switches roles, e.g. to see that a reader cannot write.
 * <p>
 * Also names the operations inherited from {@link AbstractEntityController} after their resource, e.g. {@code getPet}
 * and {@code getOwner}. By default they would all share the base method name and be told apart only by a suffix
 * ({@code get}, {@code get_1}), which is what clients generated from the OpenAPI document would use as method names.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Pet Management API",
                version = "v1",
                description = "Manage owners and their pets. Every endpoint requires HTTP Basic authentication: "
                        + "click \"Authorize\" and log in as a user from application.yml. "
                        + "READER can read, WRITER can create, update and delete, ADMIN can do both."),
        security = @SecurityRequirement(name = OpenApiConfig.BASIC_AUTH))
@SecurityScheme(
        name = OpenApiConfig.BASIC_AUTH,
        type = SecuritySchemeType.HTTP,
        scheme = "basic",
        description = "Username and password of one of the users configured in application.yml.")
public class OpenApiConfig {

    /**
     * The name of the security scheme in the OpenAPI document.
     */
    static final String BASIC_AUTH = "basicAuth";

    /**
     * Gives the endpoints inherited from {@link AbstractEntityController} resource-specific operation IDs:
     * {@code get} becomes {@code getPet}, {@code delete} {@code deletePet}, {@code history} {@code getPetHistory} and
     * {@code asOf} {@code getPetAsOf} (and the same for owners).
     *
     * @return The customizer.
     */
    @Bean
    public OperationCustomizer inheritedOperationIds() {
        return (operation, handlerMethod) -> {
            if (handlerMethod.getMethod().getDeclaringClass() == AbstractEntityController.class) {
                String resource = handlerMethod.getBeanType().getSimpleName().replace("Controller", "");
                String method = handlerMethod.getMethod().getName();
                operation.setOperationId(switch (method) {
                    case "history" -> "get" + resource + "History";
                    case "asOf" -> "get" + resource + "AsOf";
                    default -> method + resource;
                });
            }
            return operation;
        };
    }
}
