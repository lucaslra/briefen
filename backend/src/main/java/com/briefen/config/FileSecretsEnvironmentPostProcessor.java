package com.briefen.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads Docker secrets (or any file-based secrets) and injects them as
 * environment properties before the rest of the application starts.
 *
 * <p>For each supported variable, if {@code VAR_FILE} is set (e.g.
 * {@code BRIEFEN_DATASOURCE_PASSWORD_FILE=/run/secrets/db_password}),
 * the file is read, trimmed, and injected as the base variable
 * ({@code BRIEFEN_DATASOURCE_PASSWORD}).
 *
 * <p>Setting both {@code VAR} and {@code VAR_FILE} simultaneously is an
 * error — the application fails fast with a clear message.
 *
 * <p>This follows the same convention used by official Docker images
 * (PostgreSQL, MySQL, MariaDB, etc.).
 *
 * <p>Runs before {@link DatabaseProfileActivator} (order HIGHEST_PRECEDENCE + 8)
 * so that resolved secrets are visible to all downstream processors.
 *
 * <p>Registered via {@code META-INF/spring.factories}.
 */
public class FileSecretsEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    /** Run before DatabaseProfileActivator (HIGHEST_PRECEDENCE + 9). */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 8;
    }

    /**
     * Environment variables that support the {@code _FILE} suffix convention.
     * Only variables that may contain secrets are listed here.
     */
    private static final List<String> SECRET_VARS = List.of(
            "BRIEFEN_DATASOURCE_URL",
            "BRIEFEN_DATASOURCE_USERNAME",
            "BRIEFEN_DATASOURCE_PASSWORD",
            "BRIEFEN_OPENAI_API_KEY",
            "BRIEFEN_ANTHROPIC_API_KEY",
            "BRIEFEN_WEBHOOK_URL"
    );

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> resolved = new HashMap<>();

        for (String varName : SECRET_VARS) {
            String fileVar = varName + "_FILE";
            String filePath = environment.getProperty(fileVar);
            String directValue = environment.getProperty(varName);

            if (filePath == null || filePath.isBlank()) {
                continue;
            }

            // Both direct value and _FILE set → fail fast
            if (directValue != null && !directValue.isBlank()) {
                throw new IllegalStateException(
                        "Both %s and %s are set. Use one or the other, not both.".formatted(varName, fileVar));
            }

            Path path = Path.of(filePath.trim());
            if (!Files.isReadable(path)) {
                throw new IllegalStateException(
                        "%s points to '%s' which does not exist or is not readable.".formatted(fileVar, path));
            }

            try {
                String secret = Files.readString(path).trim();
                if (secret.isEmpty()) {
                    throw new IllegalStateException(
                            "%s points to '%s' which is empty.".formatted(fileVar, path));
                }
                resolved.put(varName, secret);
            } catch (IOException e) {
                throw new IllegalStateException(
                        "Failed to read secret from %s ('%s'): %s".formatted(fileVar, path, e.getMessage()), e);
            }
        }

        if (!resolved.isEmpty()) {
            // High priority — overrides defaults but not explicit env vars (which we already checked are absent)
            environment.getPropertySources().addFirst(
                    new MapPropertySource("briefen-file-secrets", resolved));
        }
    }
}
