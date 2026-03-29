package com.briefen.service;

import com.briefen.config.OllamaProperties;
import com.briefen.exception.SummarizationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.http.HttpTimeoutException;
import java.util.Map;

@Service
public class OllamaSummarizerService {

    private static final Logger log = LoggerFactory.getLogger(OllamaSummarizerService.class);

    private static final String BASE_PROMPT = """
            You are a skilled article summarizer. Given the text of an article, produce a clear and faithful summary.

            Guidelines:
            - %s
            - Start with a brief introduction stating the article's main topic.
            - Cover the key points, arguments, and findings.
            - End with the conclusion or main takeaway if the article has one.
            - Use clear, accessible English.
            - Do NOT add information that is not present in the article.
            - Do NOT include your own opinions or commentary.
            - Do NOT include inline parenthetical citations throughout the summary paragraphs — keep the summary clean.
            - At the end of the summary, include a "Key Quotes" section with 2-4 direct, verbatim short quotes from the article that support the most important claims in the summary.
            - Each quote must be in quotation marks, attributed with brief context such as the section or speaker if known.
            - Do NOT invent dates, URLs, or any metadata not present in the article.
            - Begin the response with a single markdown H1 heading (# Title) that captures the article's topic. Use the article's own title if available, or create a concise, descriptive one.
            """;

    private static final String LENGTH_DEFAULT = "Write 3 to 6 concise paragraphs depending on the article's length and complexity.";
    private static final String LENGTH_SHORTER = "Write 1 to 2 short, concise paragraphs capturing only the most essential points.";
    private static final String LENGTH_LONGER = "Write 6 to 10 detailed paragraphs providing thorough coverage of all points, arguments, and nuances.";

    private final RestClient ollamaRestClient;
    private final OllamaProperties properties;

    public OllamaSummarizerService(RestClient ollamaRestClient, OllamaProperties properties) {
        this.ollamaRestClient = ollamaRestClient;
        this.properties = properties;
    }

    /**
     * @param modelOverride if non-null/blank, use this model instead of the configured default
     */
    public String summarize(String articleText, String lengthHint, String modelOverride) {
        return doSummarize(articleText, lengthHint, resolveModel(modelOverride));
    }

    public String summarize(String articleText, String lengthHint) {
        return doSummarize(articleText, lengthHint, properties.model());
    }

    private String resolveModel(String modelOverride) {
        return (modelOverride != null && !modelOverride.isBlank()) ? modelOverride : properties.model();
    }

    private String doSummarize(String articleText, String lengthHint, String model) {
        String lengthGuideline = switch (lengthHint != null ? lengthHint.toLowerCase() : "") {
            case "shorter" -> LENGTH_SHORTER;
            case "longer" -> LENGTH_LONGER;
            default -> LENGTH_DEFAULT;
        };
        int numPredict = switch (lengthHint != null ? lengthHint.toLowerCase() : "") {
            case "shorter" -> 384;
            case "longer" -> 2048;
            default -> 1024;
        };

        String systemPrompt = BASE_PROMPT.formatted(lengthGuideline);
        String prompt = systemPrompt + "\n\nArticle text:\n---\n" + truncateIfNeeded(articleText) + "\n---\n\nSummary:";

        Map<String, Object> request = Map.of(
                "model", model,
                "prompt", prompt,
                "stream", false,
                "options", Map.of(
                        "temperature", 0.3,
                        "num_predict", numPredict
                )
        );

        log.info("Requesting summary from Ollama (model: {}, lengthHint: {}, article length: {} chars)",
                model, lengthHint != null ? lengthHint : "default", articleText.length());

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = ollamaRestClient.post()
                    .uri("/api/generate")
                    .body(request)
                    .retrieve()
                    .body(Map.class);

            if (response == null || !response.containsKey("response")) {
                throw new SummarizationException("Ollama returned an empty or invalid response.", false);
            }

            String summary = ((String) response.get("response")).strip();
            log.info("Summary generated successfully ({} chars)", summary.length());
            return summary;

        } catch (ResourceAccessException e) {
            if (isTimeoutCause(e)) {
                log.error("Ollama request timed out", e);
                throw new SummarizationException("Summarization timed out. The article may be too long or the model is still loading.", e, true);
            }
            log.error("Failed to reach Ollama", e);
            throw new SummarizationException("Could not connect to Ollama: " + e.getMessage(), e, false);
        } catch (SummarizationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during summarization", e);
            throw new SummarizationException("Summarization failed: " + e.getMessage(), e, false);
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

    private String truncateIfNeeded(String text) {
        // gemma3:4b supports 128K tokens (~100K words). 60K chars is a safe practical limit.
        int maxChars = 60000;
        if (text.length() <= maxChars) {
            return text;
        }
        log.warn("Article text truncated from {} to {} chars", text.length(), maxChars);
        return text.substring(0, maxChars) + "\n\n[Article truncated for length]";
    }
}
