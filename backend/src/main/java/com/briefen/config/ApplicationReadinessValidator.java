package com.briefen.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

@Component
public class ApplicationReadinessValidator implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(ApplicationReadinessValidator.class);

    private final OllamaProperties ollamaProperties;
    private final String dbUrl;
    private final String dbType;

    public ApplicationReadinessValidator(
            OllamaProperties ollamaProperties,
            @Value("${spring.datasource.url}") String dbUrl,
            @Value("${briefen.db.type:sqlite}") String dbType) {
        this.ollamaProperties = ollamaProperties;
        this.dbUrl = dbUrl;
        this.dbType = dbType;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if ("sqlite".equals(dbType)) {
            checkDatabaseDirectoryWritability();
        }
        checkOllamaConnectivity();
    }

    private void checkDatabaseDirectoryWritability() {
        String filePath = dbUrl.replace("jdbc:sqlite:", "");
        int queryIndex = filePath.indexOf('?');
        if (queryIndex > 0) {
            filePath = filePath.substring(0, queryIndex);
        }

        Path dbPath = Path.of(filePath);
        Path dir = dbPath.toAbsolutePath().getParent();

        if (dir == null) {
            return;
        }

        try {
            Files.createDirectories(dir);
        } catch (Exception e) {
            log.error(
                    "Briefen cannot write to the database directory: {}. Check that the path is correct and the process has write permission.",
                    dir);
            return;
        }

        if (!Files.isWritable(dir)) {
            log.error(
                    "Briefen cannot write to the database directory: {}. Check that the path is correct and the process has write permission.",
                    dir);
        }
    }

    private void checkOllamaConnectivity() {
        String baseUrl = ollamaProperties.baseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            log.warn("Ollama base URL is not configured. Skipping connectivity check.");
            return;
        }
        String tagsUrl = baseUrl + "/api/tags";
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(tagsUrl))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        try {
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() == 200) {
                log.info("Ollama is reachable at {}", baseUrl);
            } else {
                log.warn(
                        "Ollama is not reachable at {}. Summarization with local models will fail until Ollama is available.",
                        baseUrl);
            }
        } catch (Exception e) {
            log.warn(
                    "Ollama is not reachable at {}. Summarization with local models will fail until Ollama is available.",
                    baseUrl);
        }
    }
}
