package com.briefen.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Always-on HTTP Basic Auth backed by the users table.
 *
 * Authentication is required for all endpoints except /actuator/health
 * (used by Docker HEALTHCHECK and reverse-proxy health probes) and
 * /api/setup/** (used by the browser-based first-run setup flow).
 *
 * The initial admin account is created through the browser-based setup
 * flow ({@link com.briefen.service.SetupService}).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // SecurityHeadersFilter already writes all security headers — disable Spring Security's
        // duplicate header writing to avoid response header conflicts.
        http.headers(headers -> headers.disable());

        // CSRF protection is not applicable: stateless REST API with HTTP Basic Auth.
        // The API has no session cookies — every request carries an Authorization header,
        // so cross-site requests cannot be authenticated by a third-party page.
        // codeql[java/spring-disabled-csrf-protection]
        http.csrf(csrf -> csrf.disable());

        http
                .authorizeHttpRequests(auth -> auth
                        // Static frontend assets and health probe are public.
                        // Only /api/** requires authentication.
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/api/setup/**", "/api/setup").permitAll()
                        .requestMatchers("/", "/index.html", "/assets/**", "/favicon.svg", "/favicon.ico").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().authenticated()
                )
                // Use HTTP Basic for credential transport, but suppress the WWW-Authenticate
                // response header so the browser does not show its native login dialog.
                // The React frontend handles 401s and renders its own login screen.
                .httpBasic(basic -> basic.authenticationEntryPoint(
                        (request, response, ex) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED)
                ));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
