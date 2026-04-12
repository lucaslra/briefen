package com.briefen.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Activates the "sqlite" or "postgres" Spring profile based on the BRIEFEN_DB_TYPE
 * environment variable, and sets the corresponding datasource properties.
 *
 * Defaults to "sqlite" when the variable is absent or blank.
 * If "sqlite" or "postgres" is already among the active profiles (e.g. set
 * explicitly by a test via @ActiveProfiles), this post-processor does not
 * add a profile but still sets the datasource properties.
 *
 * Runs before ConfigDataEnvironmentPostProcessor (order HIGHEST_PRECEDENCE + 10)
 * so that profile-specific application-{profile}.yml files are loaded correctly.
 *
 * Registered via META-INF/spring.factories.
 */
public class DatabaseProfileActivator implements EnvironmentPostProcessor, Ordered {

    /** Run before ConfigDataEnvironmentPostProcessor (HIGHEST_PRECEDENCE + 10). */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 9;
    }

    private static final Set<String> DB_PROFILES = Set.of("sqlite", "postgres");

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        // If a database profile is already active (e.g. set by @ActiveProfiles in a test), respect it
        boolean alreadySet = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(DB_PROFILES::contains);

        String dbType = environment.getProperty("BRIEFEN_DB_TYPE", "sqlite").trim().toLowerCase();
        if (dbType.isEmpty()) {
            dbType = "sqlite";
        }

        // If a test explicitly set "postgres" profile but BRIEFEN_DB_TYPE is unset, honour the profile
        if (alreadySet && Arrays.asList(environment.getActiveProfiles()).contains("postgres")) {
            dbType = "postgres";
        }

        Map<String, Object> props = new HashMap<>();
        props.put("briefen.db.type", dbType);

        if ("sqlite".equals(dbType)) {
            String dbPath = environment.getProperty("BRIEFEN_DB_PATH", "./data/briefen.db");
            props.put("spring.datasource.url", "jdbc:sqlite:" + dbPath);
            props.put("spring.datasource.driver-class-name", "org.sqlite.JDBC");
            props.put("spring.jpa.database-platform", "org.hibernate.community.dialect.SQLiteDialect");
        } else if ("postgres".equals(dbType)) {
            String url = environment.getProperty("BRIEFEN_DATASOURCE_URL", "");
            String username = environment.getProperty("BRIEFEN_DATASOURCE_USERNAME", "");
            String password = environment.getProperty("BRIEFEN_DATASOURCE_PASSWORD", "");
            if (!url.isEmpty()) {
                props.put("spring.datasource.url", url);
            }
            if (!username.isEmpty()) {
                props.put("spring.datasource.username", username);
            }
            if (!password.isEmpty()) {
                props.put("spring.datasource.password", password);
            }
            props.put("spring.datasource.driver-class-name", "org.postgresql.Driver");
            props.put("spring.jpa.database-platform", "org.hibernate.dialect.PostgreSQLDialect");
        }

        // Low priority — can be overridden by application.yml, test properties, etc.
        environment.getPropertySources().addLast(
                new MapPropertySource("briefen-db-defaults", props));

        if (!alreadySet) {
            environment.addActiveProfile(dbType);
        }
    }
}
