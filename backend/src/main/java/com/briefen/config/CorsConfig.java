package com.briefen.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

/**
 * CORS configuration for the Briefen API.
 *
 * By default, no cross-origin requests are allowed. Set the
 * {@code BRIEFEN_CORS_ALLOWED_ORIGINS} environment variable to a comma-separated list
 * of allowed origins (wildcards supported, e.g. {@code moz-extension://*}).
 *
 * Example:
 * <pre>
 *   BRIEFEN_CORS_ALLOWED_ORIGINS=moz-extension://*,http://localhost:5173
 * </pre>
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${briefen.cors.allowed-origins:}")
    private String allowedOriginsRaw;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] patterns = parseOrigins(allowedOriginsRaw);
        if (patterns.length == 0) {
            return;
        }
        registry.addMapping("/api/**")
                .allowedOriginPatterns(patterns)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }

    private String[] parseOrigins(String raw) {
        if (raw == null || raw.isBlank()) {
            return new String[0];
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toArray(String[]::new);
    }
}
