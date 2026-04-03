package com.briefen.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "anthropic")
public record AnthropicProperties(
        String baseUrl,
        Duration timeout
) {
    public AnthropicProperties {
        if (baseUrl == null) baseUrl = "https://api.anthropic.com";
        if (timeout == null) timeout = Duration.ofSeconds(120);
    }
}
