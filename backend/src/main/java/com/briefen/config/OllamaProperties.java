package com.briefen.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "ollama")
public record OllamaProperties(
        String baseUrl,
        String model,
        Duration timeout,
        Integer numPredictShorter,
        Integer numPredictDefault,
        Integer numPredictLonger
) {
    public OllamaProperties {
        if (baseUrl == null) baseUrl = "http://localhost:11434";
        if (model == null) model = "gemma2:2b";
        if (timeout == null) timeout = Duration.ofSeconds(60);
        if (numPredictShorter == null) numPredictShorter = 384;
        if (numPredictDefault == null) numPredictDefault = 1024;
        if (numPredictLonger == null) numPredictLonger = 2048;
    }
}
