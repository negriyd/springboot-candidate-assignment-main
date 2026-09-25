package com.interzero.TestServer.configuration;

import com.interzero.TestServer.error.SecurityErrorHandler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
 * Access is role based:
 * <ul>
 *     <li>{@link Role#READER} - may only read data (GET/HEAD).</li>
 *     <li>{@link Role#WRITER} - may only write data (POST/PUT/PATCH/DELETE).</li>
 *     <li>{@link Role#ADMIN} - may do everything.</li>
 * </ul>
 * The health check endpoint {@code GET /} is available to any authenticated user.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, SecurityErrorHandler securityErrorHandler) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/").authenticated()
                        .requestMatchers(HttpMethod.GET, "/**").hasAnyRole(Role.READER.name(), Role.ADMIN.name())
                        .requestMatchers(HttpMethod.HEAD, "/**").hasAnyRole(Role.READER.name(), Role.ADMIN.name())
                        .requestMatchers(HttpMethod.POST, "/**").hasAnyRole(Role.WRITER.name(), Role.ADMIN.name())
                        .requestMatchers(HttpMethod.PUT, "/**").hasAnyRole(Role.WRITER.name(), Role.ADMIN.name())
                        .requestMatchers(HttpMethod.PATCH, "/**").hasAnyRole(Role.WRITER.name(), Role.ADMIN.name())
                        .requestMatchers(HttpMethod.DELETE, "/**").hasAnyRole(Role.WRITER.name(), Role.ADMIN.name())
                        .anyRequest().hasRole(Role.ADMIN.name())
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
