package com.briefen.controller;

import com.briefen.config.OllamaProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Exposes available Ollama models and the server default.
 */
@RestController
@RequestMapping("/api/models")
public class ModelsController {

    private final OllamaProperties ollamaProperties;

    public ModelsController(OllamaProperties ollamaProperties) {
        this.ollamaProperties = ollamaProperties;
    }

    @GetMapping
    public Map<String, Object> list() {
        return Map.of(
                "defaultModel", ollamaProperties.model(),
                "models", List.of(
                        Map.of(
                                "id", "gemma2:2b",
                                "name", "Gemma 2 (2B)",
                                "description", "Lightweight and fast. Great for quick summaries with lower resource usage."
                        ),
                        Map.of(
                                "id", "gemma3:4b",
                                "name", "Gemma 3 (4B)",
                                "description", "More capable with richer understanding. Better at nuance, structure, and source attribution."
                        )
                )
        );
    }
}
