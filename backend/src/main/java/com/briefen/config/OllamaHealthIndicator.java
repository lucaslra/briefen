package com.briefen.config;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OllamaHealthIndicator implements HealthIndicator {

    private final RestClient ollamaRestClient;
    private final OllamaProperties properties;

    public OllamaHealthIndicator(RestClient ollamaRestClient, OllamaProperties properties) {
        this.ollamaRestClient = ollamaRestClient;
        this.properties = properties;
    }

    @Override
    public Health health() {
        try {
            ollamaRestClient.get()
                    .uri("/api/tags")
                    .retrieve()
                    .body(String.class);
            return Health.up()
                    .withDetail("model", properties.model())
                    .withDetail("baseUrl", properties.baseUrl())
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
