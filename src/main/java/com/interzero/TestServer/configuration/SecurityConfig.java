package com.interzero.TestServer.configuration;

import com.interzero.TestServer.error.SecurityErrorHandler;

import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Security configuration.
 * <p>
 * This class handles authentication: every API request must be authenticated. Two exceptions are declared here,
 * because they are not application controllers and so cannot use the annotations below:
 * <ul>
 *     <li>The API documentation (Swagger UI and the OpenAPI document) is public, so that users can open it and log in
 *     with its "Authorize" button. It describes the endpoints but returns no data.</li>
 *     <li>The H2 console is limited to {@link Role#ADMIN}, because it can run any SQL against the database.</li>
 * </ul>
 * Role-based access is declared on controller methods with {@code @PreAuthorize}-based annotations:
 * <ul>
 *     <li>{@link CanRead} - {@link Role#READER} and {@link Role#ADMIN}.</li>
 *     <li>{@link CanWrite} - {@link Role#WRITER} and {@link Role#ADMIN}.</li>
 * </ul>
 * Every controller method must carry one of these annotations (or its own {@code @PreAuthorize});
 * otherwise it is open to any authenticated user. This is enforced by {@code EndpointSecurityCoverageTests}.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(SecurityUsersProperties.class)
public class SecurityConfig {

    /**
     * Swagger UI and the OpenAPI document.
     */
    private static final String[] API_DOCS = {"/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**"};

    /**
     * A password that is already encoded, with the encoder id in front, e.g. <code>{bcrypt}$2a$10$...</code>.
     */
    private static final Pattern ENCODED_PASSWORD = Pattern.compile("\\{[a-zA-Z0-9-]+}.+");

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, SecurityErrorHandler securityErrorHandler) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(API_DOCS).permitAll()
                        .requestMatchers(PathRequest.toH2Console()).hasRole(Role.ADMIN.name())
                        .anyRequest().authenticated()
                )
                // The H2 console renders itself in frames, which the default X-Frame-Options: DENY would block.
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basic -> basic.authenticationEntryPoint(securityErrorHandler))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(securityErrorHandler)
                        .accessDeniedHandler(securityErrorHandler)
                )
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /**
     * The API users, created from {@link SecurityUsersProperties} ({@code app.security.users.*} in
     * {@code application.yml}). Plain-text passwords are hashed here; values that already carry an encoder
     * prefix such as <code>{bcrypt}</code> are used as they are.
     */
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder, SecurityUsersProperties properties) {
        List<UserDetails> users = properties.users().entrySet().stream()
                .map(entry -> User.withUsername(entry.getKey())
                        .password(encode(passwordEncoder, entry.getValue().password()))
                        .roles(entry.getValue().roles().stream().map(Role::name).toArray(String[]::new))
                        .build())
                .toList();
        return new InMemoryUserDetailsManager(users);
    }

    private static String encode(PasswordEncoder passwordEncoder, String password) {
        return ENCODED_PASSWORD.matcher(password).matches() ? password : passwordEncoder.encode(password);
    }
}
