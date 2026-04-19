package com.briefen.controller;

import com.briefen.model.UserSettings;
import com.briefen.persistence.SettingsPersistence;
import com.briefen.security.BriefenUserDetails;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import tools.jackson.databind.json.JsonMapper;

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
@Validated
public class ReadeckController {

    private static final Logger log = LoggerFactory.getLogger(ReadeckController.class);

    private final SettingsPersistence settingsPersistence;
    private final JsonMapper objectMapper;
    private final HttpClient httpClient;

    public ReadeckController(SettingsPersistence settingsPersistence, JsonMapper objectMapper) {
        this.settingsPersistence = settingsPersistence;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NEVER) // Prevent redirect-based SSRF
                .build();
    }

    /**
     * Returns whether Readeck is configured (has both URL and API key).
     */
    @GetMapping("/status")
    public Map<String, Object> status(@AuthenticationPrincipal BriefenUserDetails userDetails) {
        var settings = loadSettings(userDetails.userId());
        boolean configured = settings.getReadeckUrl() != null && settings.getReadeckApiKey() != null;
        return Map.of("configured", configured);
    }

    /**
     * Lists bookmarks from the Readeck instance.
     * Proxies GET /api/bookmarks with optional search/status filters.
     */
    @GetMapping("/bookmarks")
    public String listBookmarks(
            @AuthenticationPrincipal BriefenUserDetails userDetails,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status) {

        var settings = requireReadeck(userDetails.userId());
        var uriBuilder = new StringBuilder(settings.getReadeckUrl())
                .append("/api/bookmarks?page=").append(page)
                .append("&limit=").append(limit);

        if (search != null && !search.isBlank()) {
            uriBuilder.append("&search=").append(encodeParam(search));
        }
        if (status != null && !status.isBlank()) {
            uriBuilder.append("&status=").append(encodeParam(status));
        }

        return proxyGetWithRetry(uriBuilder.toString(), settings.getReadeckApiKey());
    }

    /**
     * Gets a single bookmark's metadata.
     */
    @GetMapping("/bookmarks/{id}")
    public String getBookmark(
            @AuthenticationPrincipal BriefenUserDetails userDetails,
            @PathVariable String id) {
        sanitizeId(id);
        var settings = requireReadeck(userDetails.userId());
        String url = settings.getReadeckUrl() + "/api/bookmarks/" + id;
        return proxyGetWithRetry(url, settings.getReadeckApiKey());
    }

    /**
     * Gets the article's text content for summarization.
     * Fetches the clean article HTML from Readeck's /api/bookmarks/{id}/article endpoint
     * (which returns only the article body, no Readeck chrome) and extracts plain text.
     */
    @GetMapping("/bookmarks/{id}/article")
    public Map<String, String> getArticleContent(
            @AuthenticationPrincipal BriefenUserDetails userDetails,
            @PathVariable String id) {
        sanitizeId(id);
        var settings = requireReadeck(userDetails.userId());

        // Fetch bookmark metadata for the title
        String metaUrl = settings.getReadeckUrl() + "/api/bookmarks/" + id;
        String metaJson = proxyGet(metaUrl, settings.getReadeckApiKey());

        try {
            @SuppressWarnings("unchecked")
            var meta = objectMapper.readValue(metaJson, java.util.Map.class);
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
            if (textContent.isEmpty()) {
                return Map.of("title", title, "text", "", "metadata", metaJson,
                        "error", "Article content appears to be empty.");
            }
            return Map.of("title", title, "text", textContent, "metadata", metaJson);

        } catch (Exception e) {
            log.error("Failed to extract Readeck article content for bookmark {}", id, e);
            return Map.of("metadata", metaJson, "error", "Could not extract article content.");
        }
    }

    private UserSettings loadSettings(String userId) {
        return settingsPersistence.findByUserId(userId)
                .orElseGet(UserSettings::new);
    }

    private UserSettings requireReadeck(String userId) {
        var settings = loadSettings(userId);
        if (settings.getReadeckUrl() == null || settings.getReadeckApiKey() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Readeck is not configured. Set both the Readeck URL and API key in Settings.");
        }
        validateReadeckUrl(settings.getReadeckUrl());
        return settings;
    }

    /**
     * Validates that the Readeck URL uses http or https to prevent non-HTTP SSRF.
     * Users may legitimately configure a local network address (e.g. http://192.168.1.x)
     * since Readeck is a self-hosted service — blocking private IPs would break normal use.
     */
    static void validateReadeckUrl(String url) {
        try {
            String scheme = URI.create(url).getScheme();
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Readeck URL must use http or https.");
            }
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Readeck URL.");
        }
    }

    private String proxyGetWithRetry(String url, String apiKey) {
        try {
            return proxyGet(url, apiKey);
        } catch (ResponseStatusException e) {
            int status = e.getStatusCode().value();
            if (status == 429 || status == 503) {
                log.info("Retrying Readeck request after {} for {}", status, sanitizeUrlForLog(url));
                try { Thread.sleep(500); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
                return proxyGet(url, apiKey);
            }
            throw e;
        }
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
            int status = response.statusCode();

            if (status == 401) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Readeck API key is invalid. Check your settings.");
            }
            if (status == 403) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Readeck denied access to this resource.");
            }
            if (status == 404) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Resource not found on Readeck.");
            }
            if (status == 429) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Readeck is rate-limiting requests. Try again shortly.");
            }
            if (status != 200) {
                log.warn("Readeck returned status {} for {}", status, sanitizeUrlForLog(url));
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Readeck returned status " + status);
            }

            return response.body();

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to reach Readeck at {}", sanitizeUrlForLog(url), e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Could not connect to Readeck: " + e.getMessage());
        }
    }

    /**
     * Validates that a bookmark ID is alphanumeric (no path traversal).
     */
    private static String sanitizeId(String id) {
        if (id == null || !id.matches("[a-zA-Z0-9_-]+")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid bookmark ID. Must be alphanumeric.");
        }
        return id;
    }

    private static String encodeParam(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }

    static String sanitizeUrlForLog(String url) {
        try {
            URI uri = URI.create(url);
            // Log only scheme + host — never path/query which may carry credentials or
            // user-controlled content that could enable log injection.
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null) return "<malformed-url>";
            return scheme + "://" + host;
        } catch (Exception e) {
            return "<malformed-url>";
        }
    }
}
