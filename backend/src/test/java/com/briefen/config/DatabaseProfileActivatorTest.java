package com.briefen.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.util.Arrays;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseProfileActivatorTest {

    private final DatabaseProfileActivator processor = new DatabaseProfileActivator();
    private final SpringApplication app = new SpringApplication();

    private StandardEnvironment envWith(Map<String, Object> props) {
        var env = new StandardEnvironment();
        env.getPropertySources().addFirst(new MapPropertySource("test", props));
        return env;
    }

    private StandardEnvironment emptyEnv() {
        return new StandardEnvironment();
    }

    // 1. Defaults to sqlite profile when BRIEFEN_DB_TYPE is absent

    @Test
    void shouldDefaultToSqliteWhenDbTypeIsAbsent() {
        var env = emptyEnv();
        processor.postProcessEnvironment(env, app);

        assertThat(env.getActiveProfiles()).contains("sqlite");
        assertThat(env.getProperty("briefen.db.type")).isEqualTo("sqlite");
    }

    // 2. Defaults to sqlite profile when BRIEFEN_DB_TYPE is blank/empty

    @Test
    void shouldDefaultToSqliteWhenDbTypeIsBlank() {
        var env = envWith(Map.of("BRIEFEN_DB_TYPE", "   "));
        processor.postProcessEnvironment(env, app);

        assertThat(env.getActiveProfiles()).contains("sqlite");
        assertThat(env.getProperty("briefen.db.type")).isEqualTo("sqlite");
    }

    // 3. Activates sqlite profile and sets SQLite datasource properties when BRIEFEN_DB_TYPE=sqlite

    @Test
    void shouldActivateSqliteProfileAndSetDatasourceProperties() {
        var env = envWith(Map.of("BRIEFEN_DB_TYPE", "sqlite"));
        processor.postProcessEnvironment(env, app);

        assertThat(env.getActiveProfiles()).contains("sqlite");
        assertThat(env.getProperty("spring.datasource.url")).isEqualTo("jdbc:sqlite:./data/briefen.db");
        assertThat(env.getProperty("spring.datasource.driver-class-name")).isEqualTo("org.sqlite.JDBC");
        assertThat(env.getProperty("spring.jpa.database-platform")).isEqualTo("org.hibernate.community.dialect.SQLiteDialect");
    }

    // 4. Activates postgres profile and sets PostgreSQL datasource properties

    @Test
    void shouldActivatePostgresProfileAndSetDatasourceProperties() {
        var env = envWith(Map.of(
                "BRIEFEN_DB_TYPE", "postgres",
                "BRIEFEN_DATASOURCE_URL", "jdbc:postgresql://localhost:5432/briefen",
                "BRIEFEN_DATASOURCE_USERNAME", "briefen",
                "BRIEFEN_DATASOURCE_PASSWORD", "secret"
        ));
        processor.postProcessEnvironment(env, app);

        assertThat(env.getActiveProfiles()).contains("postgres");
        assertThat(env.getProperty("spring.datasource.url")).isEqualTo("jdbc:postgresql://localhost:5432/briefen");
        assertThat(env.getProperty("spring.datasource.username")).isEqualTo("briefen");
        assertThat(env.getProperty("spring.datasource.password")).isEqualTo("secret");
    }

    // 5. Sets correct driver-class-name and dialect for SQLite

    @Test
    void shouldSetSqliteDriverAndDialect() {
        var env = envWith(Map.of("BRIEFEN_DB_TYPE", "sqlite"));
        processor.postProcessEnvironment(env, app);

        assertThat(env.getProperty("spring.datasource.driver-class-name")).isEqualTo("org.sqlite.JDBC");
        assertThat(env.getProperty("spring.jpa.database-platform")).isEqualTo("org.hibernate.community.dialect.SQLiteDialect");
    }

    // 6. Sets correct driver-class-name and dialect for PostgreSQL

    @Test
    void shouldSetPostgresDriverAndDialect() {
        var env = envWith(Map.of(
                "BRIEFEN_DB_TYPE", "postgres",
                "BRIEFEN_DATASOURCE_URL", "jdbc:postgresql://localhost:5432/briefen",
                "BRIEFEN_DATASOURCE_USERNAME", "user",
                "BRIEFEN_DATASOURCE_PASSWORD", "pass"
        ));
        processor.postProcessEnvironment(env, app);

        assertThat(env.getProperty("spring.datasource.driver-class-name")).isEqualTo("org.postgresql.Driver");
        assertThat(env.getProperty("spring.jpa.database-platform")).isEqualTo("org.hibernate.dialect.PostgreSQLDialect");
    }

    // 7. Custom BRIEFEN_DB_PATH is used in the SQLite JDBC URL

    @Test
    void shouldUseCustomDbPathForSqlite() {
        var env = envWith(Map.of(
                "BRIEFEN_DB_TYPE", "sqlite",
                "BRIEFEN_DB_PATH", "/custom/path/my.db"
        ));
        processor.postProcessEnvironment(env, app);

        assertThat(env.getProperty("spring.datasource.url")).isEqualTo("jdbc:sqlite:/custom/path/my.db");
    }

    // 8. Does NOT add a profile if sqlite/postgres is already active

    @Test
    void shouldNotAddProfileWhenSqliteAlreadyActive() {
        var env = emptyEnv();
        env.addActiveProfile("sqlite");

        processor.postProcessEnvironment(env, app);

        long sqliteCount = Arrays.stream(env.getActiveProfiles())
                .filter("sqlite"::equals)
                .count();
        assertThat(sqliteCount).isEqualTo(1);
        assertThat(env.getProperty("briefen.db.type")).isEqualTo("sqlite");
    }

    @Test
    void shouldNotAddProfileWhenPostgresAlreadyActive() {
        var env = emptyEnv();
        env.addActiveProfile("postgres");

        processor.postProcessEnvironment(env, app);

        long postgresCount = Arrays.stream(env.getActiveProfiles())
                .filter("postgres"::equals)
                .count();
        assertThat(postgresCount).isEqualTo(1);
    }

    // 9. Honors "postgres" active profile even when BRIEFEN_DB_TYPE is unset (test override path)

    @Test
    void shouldHonorPostgresProfileWhenDbTypeIsUnset() {
        var env = emptyEnv();
        env.addActiveProfile("postgres");

        processor.postProcessEnvironment(env, app);

        assertThat(env.getProperty("briefen.db.type")).isEqualTo("postgres");
        assertThat(env.getProperty("spring.datasource.driver-class-name")).isEqualTo("org.postgresql.Driver");
        assertThat(env.getProperty("spring.jpa.database-platform")).isEqualTo("org.hibernate.dialect.PostgreSQLDialect");
    }

    // 10. Sets briefen.db.type property correctly for both sqlite and postgres

    @Test
    void shouldSetDbTypePropertyForSqlite() {
        var env = envWith(Map.of("BRIEFEN_DB_TYPE", "sqlite"));
        processor.postProcessEnvironment(env, app);

        assertThat(env.getProperty("briefen.db.type")).isEqualTo("sqlite");
    }

    @Test
    void shouldSetDbTypePropertyForPostgres() {
        var env = envWith(Map.of("BRIEFEN_DB_TYPE", "postgres"));
        processor.postProcessEnvironment(env, app);

        assertThat(env.getProperty("briefen.db.type")).isEqualTo("postgres");
    }

    // 11. Properties are added with low priority (addLast) so they can be overridden

    @Test
    void shouldAddPropertiesWithLowPrioritySoTheyCanBeOverridden() {
        var env = new StandardEnvironment();
        // Add a higher-priority property source with an override
        env.getPropertySources().addFirst(new MapPropertySource("override",
                Map.of("spring.datasource.url", "jdbc:sqlite:/overridden.db")));

        processor.postProcessEnvironment(env, app);

        // The override should win because briefen-db-defaults is addLast
        assertThat(env.getProperty("spring.datasource.url")).isEqualTo("jdbc:sqlite:/overridden.db");

        // Verify the briefen-db-defaults source exists and is last among custom sources
        PropertySource<?> dbDefaults = env.getPropertySources().get("briefen-db-defaults");
        assertThat(dbDefaults).isNotNull();
    }

    // Edge case: BRIEFEN_DB_TYPE is case-insensitive

    @Test
    void shouldHandleUpperCaseDbType() {
        var env = envWith(Map.of("BRIEFEN_DB_TYPE", "SQLITE"));
        processor.postProcessEnvironment(env, app);

        assertThat(env.getActiveProfiles()).contains("sqlite");
        assertThat(env.getProperty("briefen.db.type")).isEqualTo("sqlite");
    }

    @Test
    void shouldHandleMixedCasePostgresDbType() {
        var env = envWith(Map.of("BRIEFEN_DB_TYPE", "Postgres"));
        processor.postProcessEnvironment(env, app);

        assertThat(env.getActiveProfiles()).contains("postgres");
        assertThat(env.getProperty("briefen.db.type")).isEqualTo("postgres");
    }

    // Edge case: postgres datasource vars are not set in environment (empty strings default)

    @Test
    void shouldNotSetDatasourceUrlWhenPostgresUrlIsEmpty() {
        var env = envWith(Map.of("BRIEFEN_DB_TYPE", "postgres"));
        processor.postProcessEnvironment(env, app);

        // Driver and dialect should still be set
        assertThat(env.getProperty("spring.datasource.driver-class-name")).isEqualTo("org.postgresql.Driver");
        // URL should not be set when BRIEFEN_DATASOURCE_URL is absent
        assertThat(env.getProperty("spring.datasource.url")).isNull();
        assertThat(env.getProperty("spring.datasource.username")).isNull();
        assertThat(env.getProperty("spring.datasource.password")).isNull();
    }
}
