package com.briefen.service;

import com.briefen.exception.SummarizationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class OpenAiSummarizerService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiSummarizerService.class);

    private final RestClient openAiRestClient;

    public OpenAiSummarizerService(RestClient openAiRestClient) {
        this.openAiRestClient = openAiRestClient;
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
            if (log.isDebugEnabled() && response.containsKey("usage")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> usage = (Map<String, Object>) response.get("usage");
                log.debug("OpenAI token usage — prompt: {}, completion: {}, total: {}",
                        usage.get("prompt_tokens"), usage.get("completion_tokens"), usage.get("total_tokens"));
            }
            return summary;

        } catch (HttpClientErrorException e) {
            throw SummarizerErrorHandler.handleHttpClientErrorException(e, "OpenAI", log);
        } catch (ResourceAccessException e) {
            throw SummarizerErrorHandler.handleResourceAccessException(e, "OpenAI", log);
        } catch (SummarizationException e) {
            throw e;
        } catch (Exception e) {
            throw SummarizerErrorHandler.handleUnexpectedException(e, "OpenAI", log);
        }
    }

}
