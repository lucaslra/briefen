package com.briefen.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "briefen")
public record BriefenProperties(String defaultPrompt) {
}
