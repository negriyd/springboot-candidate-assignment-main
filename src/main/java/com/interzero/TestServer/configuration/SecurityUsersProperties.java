package com.interzero.TestServer.configuration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.Map;
import java.util.Set;

/**
 * The API users, read from {@code app.security.users} in {@code application.yml}. Adding a user or changing a user's
 * roles only needs a configuration change:
 * <pre>
 * app:
 *   security:
 *     users:
 *       reader:
 *         password: reader-password
 *         roles: [READER]
 *       auditor:
 *         password: secret
 *         roles: [READER, WRITER]
 * </pre>
 * The map key is the username. A map is used rather than a list because Spring Boot merges maps from different
 * property sources key by key, so an environment variable such as {@code APP_SECURITY_USERS_ADMIN_PASSWORD} overrides
 * one user's password and keeps the other users. A list would be replaced as a whole.
 * <p>
 * The values are validated at startup, so a user without a password or roles, or with an unknown role, stops the
 * application with a clear error instead of silently locking that user out.
 *
 * @param users The users by username.
 */
@Validated
@ConfigurationProperties(prefix = "app.security")
public record SecurityUsersProperties(@NotEmpty Map<String, @Valid UserProperties> users) {

    /**
     * One user.
     *
     * @param password The password: plain text, which is hashed at startup, or an already encoded value with its
     *                 algorithm prefix, e.g. <code>{bcrypt}$2a$10$...</code>, which is used as is.
     * @param roles    The roles of the user; at least one. Names are case-insensitive, e.g. {@code [READER, WRITER]}.
     */
    public record UserProperties(@NotBlank String password, @NotEmpty Set<Role> roles) {
    }
}
