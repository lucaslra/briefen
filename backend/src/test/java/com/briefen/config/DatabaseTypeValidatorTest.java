package com.briefen.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DatabaseTypeValidatorTest {

    private DatabaseTypeValidator validator(String dbType, String url, String username, String password) {
        var env = new StandardEnvironment();
        // Add the db type as the EnvironmentPostProcessor would
        env.getPropertySources().addFirst(
                new org.springframework.core.env.MapPropertySource("test",
                        java.util.Map.of("briefen.db.type", dbType)));
        return new DatabaseTypeValidator(env, url, username, password);
    }

    @Test
    void shouldAcceptSqliteType() {
        var v = validator("sqlite", "", "", "");
        assertThatNoException().isThrownBy(v::validate);
    }

    @Test
    void shouldAcceptPostgresTypeWithAllVars() {
        var v = validator("postgres",
                "jdbc:postgresql://localhost:5432/briefen", "briefen", "secret");
        assertThatNoException().isThrownBy(v::validate);
    }

    @Test
    void shouldRejectInvalidDbType() {
        var v = validator("mysql", "", "", "");
        assertThatThrownBy(v::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid BRIEFEN_DB_TYPE: 'mysql'")
                .hasMessageContaining("sqlite, postgres");
    }

    @Test
    void shouldRejectPostgresWithMissingUrl() {
        var v = validator("postgres", "", "user", "pass");
        assertThatThrownBy(v::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BRIEFEN_DATASOURCE_URL");
    }

    @Test
    void shouldRejectPostgresWithMissingUsername() {
        var v = validator("postgres",
                "jdbc:postgresql://localhost:5432/briefen", "", "pass");
        assertThatThrownBy(v::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BRIEFEN_DATASOURCE_USERNAME");
    }

    @Test
    void shouldRejectPostgresWithMissingPassword() {
        var v = validator("postgres",
                "jdbc:postgresql://localhost:5432/briefen", "user", "");
        assertThatThrownBy(v::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BRIEFEN_DATASOURCE_PASSWORD");
    }

    @Test
    void shouldRejectPostgresWithAllVarsMissing() {
        var v = validator("postgres", "", "", "");
        assertThatThrownBy(v::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BRIEFEN_DATASOURCE_URL")
                .hasMessageContaining("BRIEFEN_DATASOURCE_USERNAME")
                .hasMessageContaining("BRIEFEN_DATASOURCE_PASSWORD");
    }

    @Test
    void shouldRejectPostgresWithBlankUrl() {
        var v = validator("postgres", "   ", "user", "pass");
        assertThatThrownBy(v::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BRIEFEN_DATASOURCE_URL");
    }
}
