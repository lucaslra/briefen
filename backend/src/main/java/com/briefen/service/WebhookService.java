package com.briefen.service;

import com.briefen.model.Summary;
import com.briefen.model.UserSettings;
import com.briefen.persistence.SettingsPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Delivers an outgoing webhook POST whenever a summary is persisted.
 *
 * The webhook URL is resolved at delivery time in this priority order:
 *   1. URL saved in app Settings (configurable from the browser UI)
 *   2. BRIEFEN_WEBHOOK_URL environment variable
 *
 * When neither is set this service is a no-op.
 *
 * Delivery is fire-and-forget on a virtual thread so it never adds latency to
 * the caller. Failures are logged at WARN level and otherwise silently dropped
 * so a misbehaving webhook target cannot affect summarization results.
 *
 * Compatible with Home Assistant webhooks, ntfy, Gotify, Apprise, and any
 * HTTP endpoint that accepts a JSON POST body.
 */
@Service
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

    private final String envWebhookUrl;
    private final SettingsPersistence settingsPersistence;
    private final RestClient restClient;

    public WebhookService(
            @Value("${briefen.webhook.url:}") String envWebhookUrl,
            SettingsPersistence settingsPersistence) {
        this.envWebhookUrl = envWebhookUrl;
        this.settingsPersistence = settingsPersistence;

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(10));

        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    /**
     * Enqueues a webhook delivery on a virtual thread.
     * Returns immediately; never throws.
     */
    public void send(Summary summary) {
        String url = resolveUrl();
        if (url == null) return;
        Thread.ofVirtual().start(() -> deliver(summary, url));
    }

    /**
     * Resolves the effective webhook URL.
     * Settings UI value takes precedence over the environment variable.
     */
    private String resolveUrl() {
        String settingsUrl = settingsPersistence.findDefault()
                .map(UserSettings::getWebhookUrl)
                .filter(url -> url != null && !url.isBlank())
                .orElse(null);
        if (settingsUrl != null) return settingsUrl;
        return (envWebhookUrl != null && !envWebhookUrl.isBlank()) ? envWebhookUrl : null;
    }

    private void deliver(Summary summary, String webhookUrl) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("event",     "summary.completed");
            payload.put("id",        summary.getId()        != null ? summary.getId()        : "");
            payload.put("url",       summary.getUrl()       != null ? summary.getUrl()       : "");
            payload.put("title",     summary.getTitle()     != null ? summary.getTitle()     : "");
            payload.put("model",     summary.getModelUsed() != null ? summary.getModelUsed() : "");
            payload.put("createdAt", summary.getCreatedAt() != null ? summary.getCreatedAt().toString() : "");

            restClient.post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

            log.debug("Webhook delivered successfully to {} for summary '{}'",
                    webhookUrl, summary.getTitle());

        } catch (Exception e) {
            log.warn("Webhook delivery to {} failed (summary '{}'): {}",
                    webhookUrl, summary.getTitle(), e.getMessage());
        }
    }
}
