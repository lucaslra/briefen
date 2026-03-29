package com.briefen.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "openai")
public record OpenAiProperties(
        String baseUrl,
        Duration timeout
) {
    public OpenAiProperties {
        if (baseUrl == null) baseUrl = "https://api.openai.com";
        if (timeout == null) timeout = Duration.ofSeconds(120);
    }
}
