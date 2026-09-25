package com.interzero.TestServer.configuration;

import com.interzero.TestServer.error.SecurityErrorHandler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

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
public class SecurityConfig {

    /**
     * Swagger UI and the OpenAPI document.
     */
    private static final String[] API_DOCS = {"/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**"};

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
     * One in-memory user per role. Usernames and passwords are defined in {@code application.properties}.
     */
    @Bean
    public UserDetailsService userDetailsService(
            PasswordEncoder passwordEncoder,
            @Value("${app.security.reader.username}") String readerUsername,
            @Value("${app.security.reader.password}") String readerPassword,
            @Value("${app.security.writer.username}") String writerUsername,
            @Value("${app.security.writer.password}") String writerPassword,
            @Value("${app.security.admin.username}") String adminUsername,
            @Value("${app.security.admin.password}") String adminPassword) {
        return new InMemoryUserDetailsManager(
                User.withUsername(readerUsername)
                        .password(passwordEncoder.encode(readerPassword))
                        .roles(Role.READER.name())
                        .build(),
                User.withUsername(writerUsername)
                        .password(passwordEncoder.encode(writerPassword))
                        .roles(Role.WRITER.name())
                        .build(),
                User.withUsername(adminUsername)
                        .password(passwordEncoder.encode(adminPassword))
                        .roles(Role.ADMIN.name())
                        .build()
        );
    }
}
