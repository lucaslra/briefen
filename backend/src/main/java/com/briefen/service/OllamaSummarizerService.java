package com.briefen.service;

import com.briefen.config.OllamaProperties;
import com.briefen.exception.SummarizationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class OllamaSummarizerService {

    private static final Logger log = LoggerFactory.getLogger(OllamaSummarizerService.class);

    private final RestClient ollamaRestClient;
    private final OllamaProperties properties;

    public OllamaSummarizerService(RestClient ollamaRestClient, OllamaProperties properties) {
        this.ollamaRestClient = ollamaRestClient;
        this.properties = properties;
    }

    /**
     * @param modelOverride if non-null/blank, use this model instead of the configured default
     * @param customPrompt  user or deployment-level custom prompt (nullable)
     * @param deploymentPrompt deployment-wide default prompt from env var (nullable)
     */
    public String summarize(String articleText, String lengthHint, String modelOverride,
                            String customPrompt, String deploymentPrompt) {
        return doSummarize(articleText, lengthHint, resolveModel(modelOverride), customPrompt, deploymentPrompt);
    }

    public String summarize(String articleText, String lengthHint, String modelOverride) {
        return doSummarize(articleText, lengthHint, resolveModel(modelOverride), null, null);
    }

    public String summarize(String articleText, String lengthHint) {
        return doSummarize(articleText, lengthHint, properties.model(), null, null);
    }

    private String resolveModel(String modelOverride) {
        return (modelOverride != null && !modelOverride.isBlank()) ? modelOverride : properties.model();
    }

    private String doSummarize(String articleText, String lengthHint, String model,
                               String customPrompt, String deploymentPrompt) {
        int numPredict = switch (lengthHint != null ? lengthHint.toLowerCase() : "") {
            case "shorter" -> properties.numPredictShorter();
            case "longer" -> properties.numPredictLonger();
            default -> properties.numPredictDefault();
        };

        String systemPrompt = PromptBuilder.build(customPrompt, deploymentPrompt, lengthHint);
        String prompt = systemPrompt + "\n\nArticle text:\n---\n" + articleText + "\n---\n\nSummary:";

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
            throw SummarizerErrorHandler.handleResourceAccessException(e, "Ollama", log);
        } catch (HttpServerErrorException e) {
            throw SummarizerErrorHandler.handleHttpServerErrorException(e, "Ollama", log);
        } catch (SummarizationException e) {
            throw e;
        } catch (Exception e) {
            throw SummarizerErrorHandler.handleUnexpectedException(e, "Ollama", log);
        }
    }

}
