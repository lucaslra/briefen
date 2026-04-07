package com.briefen.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Optional HTTP Basic Auth.
 *
 * Authentication is only enforced when both BRIEFEN_AUTH_USERNAME and
 * BRIEFEN_AUTH_PASSWORD environment variables (or application properties) are set
 * to non-blank values. When either is absent the app is open, preserving the
 * default local-use behaviour.
 *
 * The /actuator/health endpoint is always public so that the Docker HEALTHCHECK
 * and reverse-proxy health probes continue to work without credentials.
 *
 * Spring Security's built-in header writing is disabled because SecurityHeadersFilter
 * already sets all required security headers on every response.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final String username;
    private final String password;

    public SecurityConfig(
            @Value("${briefen.auth.username:}") String username,
            @Value("${briefen.auth.password:}") String password) {
        this.username = username;
        this.password = password;
    }

    private boolean isAuthEnabled() {
        return username != null && !username.isBlank()
                && password != null && !password.isBlank();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // SecurityHeadersFilter already writes all security headers — disable Spring Security's
        // duplicate header writing to avoid response header conflicts.
        http.headers(headers -> headers.disable());

        // CSRF protection is not applicable: this is a stateless REST API using HTTP Basic Auth.
        // No session cookies are issued, so there is nothing for a CSRF attack to exploit.
        http.csrf(csrf -> csrf.disable());

        if (isAuthEnabled()) {
            http
                    .authorizeHttpRequests(auth -> auth
                            // Health endpoint must remain public for Docker HEALTHCHECK
                            .requestMatchers("/actuator/health").permitAll()
                            .anyRequest().authenticated()
                    )
                    .httpBasic(Customizer.withDefaults());
        } else {
            // Auth not configured — permit everything (default local-use behaviour)
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        }

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        if (!isAuthEnabled()) {
            // Return an empty manager — no one can authenticate, but auth is not required either
            return new InMemoryUserDetailsManager();
        }

        PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        UserDetails user = User.builder()
                .username(username)
                .password(encoder.encode(password))
                .roles("USER")
                .build();
        return new InMemoryUserDetailsManager(user);
    }
}
