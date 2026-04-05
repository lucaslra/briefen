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
public class OpenAiSummarizerService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiSummarizerService.class);

    private static final String SYSTEM_PROMPT = """
            You are a skilled article summarizer. Produce a clear and faithful summary of the article provided by the user.

            Guidelines:
            - %s
            - Start with a brief introduction stating the article's main topic.
            - Cover the key points, arguments, and findings.
            - End with the conclusion or main takeaway if the article has one.
            - Use clear, accessible English and markdown formatting.
            - Do NOT add information that is not present in the article.
            - Do NOT include your own opinions or commentary.
            - Do NOT include inline parenthetical citations — keep the summary clean.
            - At the end, include a "Key Quotes" section with 2-4 direct, verbatim short quotes from the article that support the most important claims.
            - Each quote must be in quotation marks, attributed with brief context such as the section or speaker if known.
            - Do NOT invent dates, URLs, or any metadata not present in the article.
            - Begin the response with a single markdown H1 heading (# Title) that captures the article's topic. Use the article's own title if available, or create a concise, descriptive one.""";

    private static final String LENGTH_DEFAULT = "Write 3 to 6 concise paragraphs depending on the article's length and complexity.";
    private static final String LENGTH_SHORTER = "Write 1 to 2 short, concise paragraphs capturing only the most essential points.";
    private static final String LENGTH_LONGER = "Write 6 to 10 detailed paragraphs providing thorough coverage of all points, arguments, and nuances.";

    private final RestClient openAiRestClient;

    public OpenAiSummarizerService(RestClient openAiRestClient) {
        this.openAiRestClient = openAiRestClient;
    }

    public String summarize(String articleText, String lengthHint, String model, String apiKey) {
        String lengthGuideline = switch (lengthHint != null ? lengthHint.toLowerCase() : "") {
            case "shorter" -> LENGTH_SHORTER;
            case "longer" -> LENGTH_LONGER;
            default -> LENGTH_DEFAULT;
        };
        int maxTokens = switch (lengthHint != null ? lengthHint.toLowerCase() : "") {
            case "shorter" -> 800;
            case "longer" -> 4096;
            default -> 2048;
        };

        String systemMessage = SYSTEM_PROMPT.formatted(lengthGuideline);
        String userMessage = "Summarize this article:\n\n" + articleText;

        var requestMap = new java.util.HashMap<String, Object>();
        requestMap.put("model", model);
        requestMap.put("messages", List.of(
                Map.of("role", "system", "content", systemMessage),
                Map.of("role", "user", "content", userMessage)
        ));
        requestMap.put("max_completion_tokens", maxTokens);

        // Only older non-reasoning models support custom temperature
        if (model.startsWith("gpt-4")) {
            requestMap.put("temperature", 0.3);
        }

        log.info("Requesting summary from OpenAI (model: {}, lengthHint: {}, article length: {} chars)",
                model, lengthHint != null ? lengthHint : "default", articleText.length());

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = openAiRestClient.post()
                    .uri("/v1/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .body(requestMap)
                    .retrieve()
                    .body(Map.class);

            if (response == null || !response.containsKey("choices")) {
                throw new SummarizationException("OpenAI returned an empty or invalid response.", false);
            }

            @SuppressWarnings("unchecked")
            var choices = (List<Map<String, Object>>) response.get("choices");
            if (choices.isEmpty()) {
                throw new SummarizationException("OpenAI returned no choices.", false);
            }

            var firstChoice = choices.getFirst();
            String finishReason = (String) firstChoice.get("finish_reason");

            @SuppressWarnings("unchecked")
            var message = (Map<String, Object>) firstChoice.get("message");
            Object contentObj = message.get("content");
            String summary = contentObj != null ? ((String) contentObj).strip() : "";

            if (summary.isEmpty()) {
                log.warn("OpenAI returned empty content (finish_reason: {}, full response: {})", finishReason, response);
                // For reasoning models, check if there's a refusal
                Object refusal = message.get("refusal");
                if (refusal != null) {
                    throw new SummarizationException("OpenAI refused to generate: " + refusal, false);
                }
                throw new SummarizationException("OpenAI returned an empty summary (finish_reason: " + finishReason + "). Try a different model.", false);
            }

            log.info("OpenAI summary generated successfully ({} chars)", summary.length());
            return summary;

        } catch (HttpClientErrorException e) {
            log.error("OpenAI API error: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode().value() == 401) {
                throw new SummarizationException("Invalid OpenAI API key. Please check your key in Settings.", false);
            }
            if (e.getStatusCode().value() == 429) {
                throw new SummarizationException("OpenAI rate limit exceeded. Please wait a moment and try again.", e, true);
            }
            throw new SummarizationException("OpenAI API error: " + e.getResponseBodyAsString(), e, false);
        } catch (ResourceAccessException e) {
            if (isTimeoutCause(e)) {
                log.error("OpenAI request timed out", e);
                throw new SummarizationException("OpenAI request timed out. The article may be too long.", e, true);
            }
            log.error("Failed to reach OpenAI", e);
            throw new SummarizationException("Could not connect to OpenAI: " + e.getMessage(), e, false);
        } catch (SummarizationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during OpenAI summarization", e);
            throw new SummarizationException("OpenAI summarization failed: " + e.getMessage(), e, false);
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
