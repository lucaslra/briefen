package com.briefen.service;

import com.briefen.exception.SummarizationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.http.HttpTimeoutException;
import java.util.List;
import java.util.Map;

@Service
public class AnthropicSummarizerService {

    private static final Logger log = LoggerFactory.getLogger(AnthropicSummarizerService.class);

    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final RestClient anthropicRestClient;

    public AnthropicSummarizerService(RestClient anthropicRestClient) {
        this.anthropicRestClient = anthropicRestClient;
    }

    public String summarize(String articleText, String lengthHint, String model, String apiKey,
                            String customPrompt, String deploymentPrompt) {
        int maxTokens = switch (lengthHint != null ? lengthHint.toLowerCase() : "") {
            case "shorter" -> 800;
            case "longer" -> 4096;
            default -> 2048;
        };

        String systemMessage = PromptBuilder.build(customPrompt, deploymentPrompt, lengthHint);
        String userMessage = "Summarize this article:\n\n" + articleText;

        Map<String, Object> requestMap = Map.of(
                "model", model,
                "max_tokens", maxTokens,
                "system", systemMessage,
                "messages", List.of(
                        Map.of("role", "user", "content", userMessage)
                )
        );

        log.info("Requesting summary from Anthropic (model: {}, lengthHint: {}, article length: {} chars)",
                model, lengthHint != null ? lengthHint : "default", articleText.length());

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = anthropicRestClient.post()
                    .uri("/v1/messages")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", ANTHROPIC_VERSION)
                    .body(requestMap)
                    .retrieve()
                    .body(Map.class);

            if (response == null || !response.containsKey("content")) {
                throw new SummarizationException("Anthropic returned an empty or invalid response.", false);
            }

            @SuppressWarnings("unchecked")
            var contentBlocks = (List<Map<String, Object>>) response.get("content");
            if (contentBlocks.isEmpty()) {
                throw new SummarizationException("Anthropic returned no content.", false);
            }

            var firstBlock = contentBlocks.getFirst();
            String summary = firstBlock.get("text") != null ? ((String) firstBlock.get("text")).strip() : "";

            if (summary.isEmpty()) {
                String stopReason = (String) response.get("stop_reason");
                log.warn("Anthropic returned empty content (stop_reason: {}, full response: {})", stopReason, response);
                throw new SummarizationException("Anthropic returned an empty summary (stop_reason: " + stopReason + "). Try a different model.", false);
            }

            log.info("Anthropic summary generated successfully ({} chars)", summary.length());
            return summary;

        } catch (HttpClientErrorException e) {
            log.error("Anthropic API error: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode().value() == 401) {
                throw new SummarizationException("Invalid Anthropic API key. Please check your key in Settings.", false);
            }
            if (e.getStatusCode().value() == 429) {
                throw new SummarizationException("Anthropic rate limit exceeded. Please wait a moment and try again.", e, true);
            }
            throw new SummarizationException("Anthropic request failed with status " + e.getStatusCode().value() + ". Check server logs for details.", e, false);
        } catch (ResourceAccessException e) {
            if (isTimeoutCause(e)) {
                log.error("Anthropic request timed out", e);
                throw new SummarizationException("Anthropic request timed out. The article may be too long.", e, true);
            }
            log.error("Failed to reach Anthropic", e);
            throw new SummarizationException("Could not connect to Anthropic: " + e.getMessage(), e, false);
        } catch (SummarizationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during Anthropic summarization", e);
            throw new SummarizationException("Anthropic summarization failed: " + e.getMessage(), e, false);
        }
    }

    private boolean isTimeoutCause(Throwable e) {
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof HttpTimeoutException || cause instanceof java.net.SocketTimeoutException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

}
