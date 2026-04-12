package com.briefen.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Validates database-related environment variables at startup and fails fast
 * with actionable error messages if the configuration is invalid.
 */
@Component
public class DatabaseTypeValidator {

    private static final Logger log = LoggerFactory.getLogger(DatabaseTypeValidator.class);
    private static final Set<String> VALID_DB_TYPES = Set.of("sqlite", "postgres");

    private final String dbType;
    private final String datasourceUrl;
    private final String datasourceUsername;
    private final String datasourcePassword;

    public DatabaseTypeValidator(
            Environment environment,
            @Value("${BRIEFEN_DATASOURCE_URL:}") String datasourceUrl,
            @Value("${BRIEFEN_DATASOURCE_USERNAME:}") String datasourceUsername,
            @Value("${BRIEFEN_DATASOURCE_PASSWORD:}") String datasourcePassword) {
        // Determine effective db type: check active profiles first (handles test overrides),
        // then fall back to the property set by DatabaseProfileActivator
        if (Arrays.asList(environment.getActiveProfiles()).contains("postgres")) {
            this.dbType = "postgres";
        } else {
            this.dbType = environment.getProperty("briefen.db.type", "sqlite");
        }
        this.datasourceUrl = datasourceUrl;
        this.datasourceUsername = datasourceUsername;
        this.datasourcePassword = datasourcePassword;
    }

    @PostConstruct
    public void validate() {
        if (!VALID_DB_TYPES.contains(dbType)) {
            throw new IllegalStateException(
                    "Invalid BRIEFEN_DB_TYPE: '%s'. Valid values are: sqlite, postgres".formatted(dbType));
        }

        if ("postgres".equals(dbType)) {
            List<String> missing = new ArrayList<>();
            if (datasourceUrl == null || datasourceUrl.isBlank()) {
                missing.add("BRIEFEN_DATASOURCE_URL");
            }
            if (datasourceUsername == null || datasourceUsername.isBlank()) {
                missing.add("BRIEFEN_DATASOURCE_USERNAME");
            }
            if (datasourcePassword == null || datasourcePassword.isBlank()) {
                missing.add("BRIEFEN_DATASOURCE_PASSWORD");
            }
            if (!missing.isEmpty()) {
                throw new IllegalStateException(
                        "BRIEFEN_DB_TYPE is set to 'postgres' but the following required variables are missing or blank: %s"
                                .formatted(String.join(", ", missing)));
            }
            log.info("Database type: PostgreSQL ({})", datasourceUrl);
        } else {
            log.info("Database type: SQLite");
        }
    }
}
