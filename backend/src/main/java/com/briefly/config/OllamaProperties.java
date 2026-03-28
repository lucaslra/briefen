package com.briefly.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "ollama")
public record OllamaProperties(
        String baseUrl,
        String model,
        Duration timeout
) {
    public OllamaProperties {
        if (baseUrl == null) baseUrl = "http://localhost:11434";
        if (model == null) model = "gemma2:2b";
        if (timeout == null) timeout = Duration.ofSeconds(60);
    }
}
