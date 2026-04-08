package com.briefen.controller;

import com.briefen.model.UserSettings;
import com.briefen.persistence.SettingsPersistence;
import com.briefen.security.WithMockBriefenUser;
import com.briefen.validation.UrlValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@WithMockBriefenUser
class SettingsControllerTest {

    private static final String TEST_USER_ID = "test-user-id";

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockitoBean
    private SettingsPersistence settingsPersistence;

    @MockitoBean
    private UrlValidator urlValidator;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    // ---- GET /api/settings ----

    @Test
    void shouldReturn200WithSettingsOnGet() throws Exception {
        // Arrange
        UserSettings settings = new UserSettings();
        settings.setId(TEST_USER_ID);
        settings.setDefaultLength("default");
        when(settingsPersistence.findByUserId(TEST_USER_ID)).thenReturn(Optional.of(settings));

        // Act & Assert
        mockMvc.perform(get("/api/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultLength").value("default"));
    }

    @Test
    void shouldReturnDefaultSettingsWhenNoneStoredOnGet() throws Exception {
        // Arrange — no settings in DB → controller creates default for the user
        when(settingsPersistence.findByUserId(TEST_USER_ID)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultLength").exists());
    }

    @Test
    void shouldMaskOpenAiApiKeyInResponse() throws Exception {
        // Arrange
        UserSettings settings = new UserSettings();
        settings.setId(TEST_USER_ID);
        settings.setOpenaiApiKey("sk-abc123xyz789");
        when(settingsPersistence.findByUserId(TEST_USER_ID)).thenReturn(Optional.of(settings));

        // Act & Assert — key is present but masked (not the raw value)
        mockMvc.perform(get("/api/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openaiApiKey").value("sk-a...z789"));
    }

    @Test
    void shouldReturnNullOpenAiApiKeyWhenNotSet() throws Exception {
        // Arrange
        UserSettings settings = new UserSettings();
        settings.setId(TEST_USER_ID);
        when(settingsPersistence.findByUserId(TEST_USER_ID)).thenReturn(Optional.of(settings));

        // Act & Assert — no key stored → null in response
        mockMvc.perform(get("/api/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openaiApiKey").doesNotExist());
    }

    // ---- PUT /api/settings ----

    @Test
    void shouldReturn200OnSettingsUpdate() throws Exception {
        // Arrange
        UserSettings existing = new UserSettings();
        existing.setId(TEST_USER_ID);
        when(settingsPersistence.findByUserId(TEST_USER_ID)).thenReturn(Optional.of(existing));
        when(settingsPersistence.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act & Assert
        mockMvc.perform(put("/api/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"defaultLength":"shorter","notificationsEnabled":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultLength").value("shorter"));
    }

    @Test
    void shouldReturn400WhenReadeckUrlIsInvalid() throws Exception {
        // Arrange
        UserSettings existing = new UserSettings();
        existing.setId(TEST_USER_ID);
        when(settingsPersistence.findByUserId(TEST_USER_ID)).thenReturn(Optional.of(existing));
        // Simulate the validator throwing for an ftp:// URL
        org.mockito.Mockito.doThrow(new com.briefen.exception.InvalidUrlException("URL must use HTTP or HTTPS scheme."))
                .when(urlValidator).validateServiceUrl("ftp://bad.url");

        // Act & Assert
        mockMvc.perform(put("/api/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"readeckUrl":"ftp://bad.url"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldAcceptValidReadeckUrl() throws Exception {
        // Arrange
        UserSettings existing = new UserSettings();
        existing.setId(TEST_USER_ID);
        when(settingsPersistence.findByUserId(TEST_USER_ID)).thenReturn(Optional.of(existing));
        // urlValidator.validateServiceUrl does nothing (no exception) for a valid URL
        when(settingsPersistence.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act & Assert
        mockMvc.perform(put("/api/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"readeckUrl":"http://192.168.1.100:8080"}
                                """))
                .andExpect(status().isOk());
    }
}
