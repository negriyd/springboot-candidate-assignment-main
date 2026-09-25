package com.interzero.TestServer.configuration;

import com.interzero.TestServer.error.SecurityErrorHandler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
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
 * This class handles authentication only: every request (except the H2 console) must be authenticated.
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

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, SecurityErrorHandler securityErrorHandler) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/h2-console/**").permitAll()
                        .anyRequest().authenticated()
                )
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
