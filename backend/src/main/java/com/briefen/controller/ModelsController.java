package com.briefen.controller;

import com.briefen.config.OllamaProperties;
import com.briefen.persistence.SettingsPersistence;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Exposes available models grouped by provider.
 */
@RestController
@RequestMapping("/api/models")
public class ModelsController {

    private final OllamaProperties ollamaProperties;
    private final SettingsPersistence settingsPersistence;

    public ModelsController(OllamaProperties ollamaProperties, SettingsPersistence settingsPersistence) {
        this.ollamaProperties = ollamaProperties;
        this.settingsPersistence = settingsPersistence;
    }

    @GetMapping
    public Map<String, Object> list() {
        var settings = settingsPersistence.findDefault();
        boolean hasOpenAiKey = settings
                .map(s -> s.getOpenaiApiKey() != null && !s.getOpenaiApiKey().isBlank())
                .orElse(false);
        boolean hasAnthropicKey = settings
                .map(s -> s.getAnthropicApiKey() != null && !s.getAnthropicApiKey().isBlank())
                .orElse(false);

        List<Map<String, Object>> providers = new ArrayList<>();

        // Ollama — always available
        providers.add(Map.of(
                "id", "ollama",
                "name", "Ollama (Local)",
                "configured", true,
                "models", List.of(
                        Map.of("id", "gemma2:2b", "name", "Gemma 2 (2B)",
                                "description", "Lightweight and fast. Great for quick summaries with lower resource usage."),
                        Map.of("id", "gemma3:4b", "name", "Gemma 3 (4B)",
                                "description", "More capable with richer understanding. Better at nuance, structure, and source attribution."),
                        Map.of("id", "llama3.2:3b", "name", "Llama 3.2 (3B)",
                                "description", "Meta's compact model. Strong reasoning and instruction-following with a good balance of speed and quality.")
                )
        ));

        // OpenAI — available only if API key is set
        providers.add(Map.of(
                "id", "openai",
                "name", "OpenAI",
                "configured", hasOpenAiKey,
                "models", List.of(
                        Map.of("id", "gpt-4.5-preview", "name", "GPT-4.5 Preview",
                                "description", "Largest non-reasoning model. Exceptional at creative, nuanced summarization."),
                        Map.of("id", "gpt-5-mini", "name", "GPT-5 Mini",
                                "description", "Next-gen compact model. Top-tier quality with fast response times."),
                        Map.of("id", "o3-mini", "name", "o3 Mini",
                                "description", "Reasoning model. Excels at complex, nuanced analysis with step-by-step thinking."),
                        Map.of("id", "o4-mini", "name", "o4 Mini",
                                "description", "Latest reasoning model. Best-in-class analysis with efficient token usage.")
                )
        ));

        // Anthropic — available only if API key is set
        providers.add(Map.of(
                "id", "anthropic",
                "name", "Anthropic",
                "configured", hasAnthropicKey,
                "models", List.of(
                        Map.of("id", "claude-opus-4-5", "name", "Claude Opus 4.5",
                                "description", "Most capable model. Exceptional at complex analysis, nuanced writing, and thorough summarization."),
                        Map.of("id", "claude-sonnet-4-5", "name", "Claude Sonnet 4.5",
                                "description", "Best balance of speed and quality. Great for everyday summarization tasks."),
                        Map.of("id", "claude-haiku-4-5", "name", "Claude Haiku 4.5",
                                "description", "Fastest and most compact. Ideal for quick summaries with low latency.")
                )
        ));

        return Map.of(
                "defaultModel", ollamaProperties.model(),
                "providers", providers
        );
    }
}
