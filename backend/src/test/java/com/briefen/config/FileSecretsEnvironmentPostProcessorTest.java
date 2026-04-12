package com.briefen.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class FileSecretsEnvironmentPostProcessorTest {

    private final FileSecretsEnvironmentPostProcessor processor = new FileSecretsEnvironmentPostProcessor();
    private final SpringApplication app = new SpringApplication();

    private StandardEnvironment envWith(Map<String, Object> props) {
        var env = new StandardEnvironment();
        env.getPropertySources().addFirst(new MapPropertySource("test", props));
        return env;
    }

    @Test
    void shouldReadSecretFromFile(@TempDir Path tmp) throws IOException {
        Path secretFile = tmp.resolve("db_password");
        Files.writeString(secretFile, "s3cret\n");

        var env = envWith(Map.of("BRIEFEN_DATASOURCE_PASSWORD_FILE", secretFile.toString()));
        processor.postProcessEnvironment(env, app);

        assertThat(env.getProperty("BRIEFEN_DATASOURCE_PASSWORD")).isEqualTo("s3cret");
    }

    @Test
    void shouldTrimWhitespaceFromSecretFile(@TempDir Path tmp) throws IOException {
        Path secretFile = tmp.resolve("api_key");
        Files.writeString(secretFile, "  sk-test-key  \n\n");

        var env = envWith(Map.of("BRIEFEN_OPENAI_API_KEY_FILE", secretFile.toString()));
        processor.postProcessEnvironment(env, app);

        assertThat(env.getProperty("BRIEFEN_OPENAI_API_KEY")).isEqualTo("sk-test-key");
    }

    @Test
    void shouldResolveMultipleSecretFiles(@TempDir Path tmp) throws IOException {
        Files.writeString(tmp.resolve("url"), "jdbc:postgresql://db:5432/briefen");
        Files.writeString(tmp.resolve("user"), "briefen");
        Files.writeString(tmp.resolve("pass"), "hunter2");

        var env = envWith(Map.of(
                "BRIEFEN_DATASOURCE_URL_FILE", tmp.resolve("url").toString(),
                "BRIEFEN_DATASOURCE_USERNAME_FILE", tmp.resolve("user").toString(),
                "BRIEFEN_DATASOURCE_PASSWORD_FILE", tmp.resolve("pass").toString()
        ));
        processor.postProcessEnvironment(env, app);

        assertThat(env.getProperty("BRIEFEN_DATASOURCE_URL")).isEqualTo("jdbc:postgresql://db:5432/briefen");
        assertThat(env.getProperty("BRIEFEN_DATASOURCE_USERNAME")).isEqualTo("briefen");
        assertThat(env.getProperty("BRIEFEN_DATASOURCE_PASSWORD")).isEqualTo("hunter2");
    }

    @Test
    void shouldFailWhenBothDirectAndFileAreSet(@TempDir Path tmp) throws IOException {
        Path secretFile = tmp.resolve("api_key");
        Files.writeString(secretFile, "sk-from-file");

        var env = envWith(Map.of(
                "BRIEFEN_OPENAI_API_KEY", "sk-from-env",
                "BRIEFEN_OPENAI_API_KEY_FILE", secretFile.toString()
        ));

        assertThatThrownBy(() -> processor.postProcessEnvironment(env, app))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BRIEFEN_OPENAI_API_KEY")
                .hasMessageContaining("BRIEFEN_OPENAI_API_KEY_FILE")
                .hasMessageContaining("not both");
    }

    @Test
    void shouldFailWhenFileDoesNotExist() {
        var env = envWith(Map.of(
                "BRIEFEN_DATASOURCE_PASSWORD_FILE", "/nonexistent/secret/file"
        ));

        assertThatThrownBy(() -> processor.postProcessEnvironment(env, app))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("does not exist or is not readable");
    }

    @Test
    void shouldFailWhenFileIsEmpty(@TempDir Path tmp) throws IOException {
        Path secretFile = tmp.resolve("empty");
        Files.writeString(secretFile, "   \n");

        var env = envWith(Map.of(
                "BRIEFEN_DATASOURCE_PASSWORD_FILE", secretFile.toString()
        ));

        assertThatThrownBy(() -> processor.postProcessEnvironment(env, app))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void shouldDoNothingWhenNoFileVarsAreSet() {
        var env = envWith(Map.of("SERVER_PORT", "9090"));

        assertThatNoException().isThrownBy(() -> processor.postProcessEnvironment(env, app));
        // No secret properties should have been injected
        assertThat(env.getProperty("BRIEFEN_DATASOURCE_PASSWORD")).isNull();
    }

    @Test
    void shouldIgnoreBlankFileVar() {
        var env = envWith(Map.of("BRIEFEN_DATASOURCE_PASSWORD_FILE", "   "));

        assertThatNoException().isThrownBy(() -> processor.postProcessEnvironment(env, app));
        assertThat(env.getProperty("BRIEFEN_DATASOURCE_PASSWORD")).isNull();
    }

    @Test
    void shouldNotConflictWhenDirectVarSetButNoFile() {
        var env = envWith(Map.of("BRIEFEN_OPENAI_API_KEY", "sk-direct"));

        assertThatNoException().isThrownBy(() -> processor.postProcessEnvironment(env, app));
        assertThat(env.getProperty("BRIEFEN_OPENAI_API_KEY")).isEqualTo("sk-direct");
    }

    @Test
    void shouldSupportAllSecretVars(@TempDir Path tmp) throws IOException {
        Map<String, Object> props = new HashMap<>();
        var vars = Map.of(
                "BRIEFEN_DATASOURCE_URL", "jdbc:postgresql://db:5432/test",
                "BRIEFEN_DATASOURCE_USERNAME", "testuser",
                "BRIEFEN_DATASOURCE_PASSWORD", "testpass",
                "BRIEFEN_OPENAI_API_KEY", "sk-openai",
                "BRIEFEN_ANTHROPIC_API_KEY", "sk-ant-anthropic",
                "BRIEFEN_WEBHOOK_URL", "https://hooks.example.com/test"
        );

        for (var entry : vars.entrySet()) {
            Path file = tmp.resolve(entry.getKey().toLowerCase());
            Files.writeString(file, entry.getValue());
            props.put(entry.getKey() + "_FILE", file.toString());
        }

        var env = envWith(props);
        processor.postProcessEnvironment(env, app);

        for (var entry : vars.entrySet()) {
            assertThat(env.getProperty(entry.getKey()))
                    .as("Expected %s to be resolved from file", entry.getKey())
                    .isEqualTo(entry.getValue());
        }
    }
}
