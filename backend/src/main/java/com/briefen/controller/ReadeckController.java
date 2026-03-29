package com.briefen.controller;

import com.briefen.model.UserSettings;
import com.briefen.repository.UserSettingsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Proxies requests to a user-configured Readeck instance.
 * The frontend never sees the Readeck API key — it stays server-side.
 */
@RestController
@RequestMapping("/api/readeck")
public class ReadeckController {

    private static final Logger log = LoggerFactory.getLogger(ReadeckController.class);

    private final UserSettingsRepository settingsRepository;
    private final HttpClient httpClient;

    public ReadeckController(UserSettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * Returns whether Readeck is configured (has both URL and API key).
     */
    @GetMapping("/status")
    public Map<String, Object> status() {
        var settings = loadSettings();
        boolean configured = settings.getReadeckUrl() != null && settings.getReadeckApiKey() != null;
        return Map.of("configured", configured);
    }

    /**
     * Lists bookmarks from the Readeck instance.
     * Proxies GET /api/bookmarks with optional search/status filters.
     */
    @GetMapping("/bookmarks")
    public String listBookmarks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status) {

        var settings = requireReadeck();
        var uriBuilder = new StringBuilder(settings.getReadeckUrl())
                .append("/api/bookmarks?page=").append(page)
                .append("&limit=").append(limit);

        if (search != null && !search.isBlank()) {
            uriBuilder.append("&search=").append(encodeParam(search));
        }
        if (status != null && !status.isBlank()) {
            uriBuilder.append("&status=").append(encodeParam(status));
        }

        return proxyGet(uriBuilder.toString(), settings.getReadeckApiKey());
    }

    /**
     * Gets a single bookmark's metadata.
     */
    @GetMapping("/bookmarks/{id}")
    public String getBookmark(@PathVariable String id) {
        var settings = requireReadeck();
        String url = settings.getReadeckUrl() + "/api/bookmarks/" + id;
        return proxyGet(url, settings.getReadeckApiKey());
    }

    /**
     * Gets the article's text content for summarization.
     * Fetches the clean article HTML from Readeck's /api/bookmarks/{id}/article endpoint
     * (which returns only the article body, no Readeck chrome) and extracts plain text.
     */
    @GetMapping("/bookmarks/{id}/article")
    public Map<String, String> getArticleContent(@PathVariable String id) {
        var settings = requireReadeck();

        // Fetch bookmark metadata for the title
        String metaUrl = settings.getReadeckUrl() + "/api/bookmarks/" + id;
        String metaJson = proxyGet(metaUrl, settings.getReadeckApiKey());

        try {
            @SuppressWarnings("unchecked")
            var meta = new com.fasterxml.jackson.databind.ObjectMapper().readValue(metaJson, java.util.Map.class);
            String title = meta.get("title") != null ? meta.get("title").toString() : "";

            // Fetch the clean article content from the /article endpoint (NOT /article.html which is a bookmark export)
            String articleUrl = settings.getReadeckUrl() + "/api/bookmarks/" + id + "/article";
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(articleUrl))
                    .header("Authorization", "Bearer " + settings.getReadeckApiKey())
                    .header("Accept", "text/html")
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();

            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String textContent = "";

            if (response.statusCode() == 200) {
                var doc = org.jsoup.Jsoup.parse(response.body());
                // The /article endpoint returns clean HTML — just paragraphs and basic formatting.
                // Extract text preserving paragraph breaks for better LLM context.
                var paragraphs = doc.select("p, h1, h2, h3, h4, h5, h6, li, blockquote, pre");
                if (!paragraphs.isEmpty()) {
                    var sb = new StringBuilder();
                    for (var el : paragraphs) {
                        String text = el.text().strip();
                        if (!text.isEmpty()) {
                            sb.append(text).append("\n\n");
                        }
                    }
                    textContent = sb.toString().strip();
                } else {
                    // Fallback: if no semantic elements found, use body text
                    textContent = doc.text().strip();
                }
            }

            log.info("Extracted article text for bookmark {} ({} chars)", id, textContent.length());
            return Map.of("title", title, "text", textContent, "metadata", metaJson);

        } catch (Exception e) {
            log.error("Failed to extract Readeck article content for bookmark {}", id, e);
            return Map.of("metadata", metaJson);
        }
    }

    private UserSettings loadSettings() {
        return settingsRepository.findById(UserSettings.DEFAULT_ID)
                .orElseGet(UserSettings::new);
    }

    private UserSettings requireReadeck() {
        var settings = loadSettings();
        if (settings.getReadeckUrl() == null || settings.getReadeckApiKey() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Readeck is not configured. Set both the Readeck URL and API key in Settings.");
        }
        return settings;
    }

    private String proxyGet(String url, String apiKey) {
        try {
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 401) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Readeck API key is invalid. Check your settings.");
            }
            if (response.statusCode() != 200) {
                log.warn("Readeck returned status {} for {}", response.statusCode(), url);
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Readeck returned status " + response.statusCode());
            }

            return response.body();

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to reach Readeck at {}", url, e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Could not connect to Readeck: " + e.getMessage());
        }
    }

    private static String encodeParam(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }
}
