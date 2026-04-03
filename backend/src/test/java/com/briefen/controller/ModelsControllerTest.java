package com.briefen.controller;

import com.briefen.config.OllamaProperties;
import com.briefen.model.UserSettings;
import com.briefen.persistence.SettingsPersistence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
class ModelsControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockitoBean
    private OllamaProperties ollamaProperties;

    @MockitoBean
    private SettingsPersistence settingsPersistence;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void shouldReturn200WithProvidersArray() throws Exception {
        // Arrange
        when(ollamaProperties.model()).thenReturn("gemma3:4b");
        when(settingsPersistence.findDefault()).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/models"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providers").isArray())
                .andExpect(jsonPath("$.defaultModel").value("gemma3:4b"));
    }

    @Test
    void shouldIncludeOllamaProviderWithModels() throws Exception {
        // Arrange
        when(ollamaProperties.model()).thenReturn("gemma3:4b");
        when(settingsPersistence.findDefault()).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/models"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providers[0].id").value("ollama"))
                .andExpect(jsonPath("$.providers[0].configured").value(true))
                .andExpect(jsonPath("$.providers[0].models").isArray());
    }

    @Test
    void shouldMarkOpenAiAsUnconfiguredWhenNoApiKey() throws Exception {
        // Arrange — no settings → no openai key
        when(ollamaProperties.model()).thenReturn("gemma3:4b");
        when(settingsPersistence.findDefault()).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/models"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providers[1].id").value("openai"))
                .andExpect(jsonPath("$.providers[1].configured").value(false));
    }

    @Test
    void shouldMarkOpenAiAsConfiguredWhenApiKeySet() throws Exception {
        // Arrange
        UserSettings settings = new UserSettings();
        settings.setOpenaiApiKey("sk-real-key-abc123");
        when(ollamaProperties.model()).thenReturn("gemma3:4b");
        when(settingsPersistence.findDefault()).thenReturn(Optional.of(settings));

        // Act & Assert
        mockMvc.perform(get("/api/models"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providers[1].id").value("openai"))
                .andExpect(jsonPath("$.providers[1].configured").value(true));
    }

    @Test
    void shouldIncludeOpenAiModelsInResponse() throws Exception {
        // Arrange
        when(ollamaProperties.model()).thenReturn("gemma3:4b");
        when(settingsPersistence.findDefault()).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/models"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providers[1].models").isArray())
                .andExpect(jsonPath("$.providers[1].models[?(@.id == 'o4-mini')]").exists());
    }
}
