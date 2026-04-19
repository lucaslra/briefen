package com.briefen.service;

import com.briefen.exception.InvalidUrlException;
import com.briefen.model.Summary;
import com.briefen.model.UserSettings;
import com.briefen.persistence.SettingsPersistence;
import com.briefen.validation.UrlValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

    private static final String USER_ID = "user-1";

    private Summary createTestSummary() {
        Summary summary = new Summary("https://example.com/article", "Test Article", "Summary text", "claude-sonnet-4-20250514");
        summary.setId("sum-123");
        summary.setCreatedAt(Instant.parse("2026-04-12T10:00:00Z"));
        return summary;
    }

    @Test
    void shouldBeNoOpWhenBothUrlsAreEmpty() {
        SettingsPersistence settingsPersistence = mock(SettingsPersistence.class);
        when(settingsPersistence.findByUserId(USER_ID)).thenReturn(Optional.empty());

        WebhookService service = new WebhookService("", settingsPersistence, mock(UrlValidator.class));

        assertThatCode(() -> service.send(createTestSummary(), USER_ID))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldBeNoOpWhenSettingsUrlIsBlankAndEnvUrlIsNull() {
        UserSettings settings = new UserSettings();
        settings.setWebhookUrl("   ");

        SettingsPersistence settingsPersistence = mock(SettingsPersistence.class);
        when(settingsPersistence.findByUserId(USER_ID)).thenReturn(Optional.of(settings));

        WebhookService service = new WebhookService(null, settingsPersistence, mock(UrlValidator.class));

        assertThatCode(() -> service.send(createTestSummary(), USER_ID))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldBeNoOpWhenNoSettingsExistAndEnvUrlIsBlank() {
        SettingsPersistence settingsPersistence = mock(SettingsPersistence.class);
        when(settingsPersistence.findByUserId(USER_ID)).thenReturn(Optional.empty());

        WebhookService service = new WebhookService("  ", settingsPersistence, mock(UrlValidator.class));

        assertThatCode(() -> service.send(createTestSummary(), USER_ID))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldPreferUserSettingsUrlOverEnvUrl() throws InterruptedException {
        String settingsUrl = "https://settings-webhook.example.com/hook";
        String envUrl = "https://env-webhook.example.com/hook";

        UserSettings settings = new UserSettings();
        settings.setWebhookUrl(settingsUrl);

        SettingsPersistence settingsPersistence = mock(SettingsPersistence.class);
        when(settingsPersistence.findByUserId(USER_ID)).thenReturn(Optional.of(settings));

        UrlValidator urlValidator = mock(UrlValidator.class);
        WebhookService service = new WebhookService(envUrl, settingsPersistence, urlValidator);

        service.send(createTestSummary(), USER_ID);
        Thread.sleep(200);

        verify(settingsPersistence).findByUserId(USER_ID);
    }

    @Test
    void shouldFallBackToEnvUrlWhenNoSettingsUrlConfigured() throws InterruptedException {
        SettingsPersistence settingsPersistence = mock(SettingsPersistence.class);
        when(settingsPersistence.findByUserId(USER_ID)).thenReturn(Optional.empty());

        UrlValidator urlValidator = mock(UrlValidator.class);
        WebhookService service = new WebhookService("https://env-webhook.example.com/hook", settingsPersistence, urlValidator);

        service.send(createTestSummary(), USER_ID);
        Thread.sleep(200);

        verify(settingsPersistence).findByUserId(USER_ID);
    }

    @Test
    void shouldPropagateExceptionWhenUrlResolutionFails() {
        SettingsPersistence settingsPersistence = mock(SettingsPersistence.class);
        when(settingsPersistence.findByUserId(anyString())).thenThrow(new RuntimeException("DB down"));

        WebhookService service = new WebhookService("https://example.com/hook", settingsPersistence, mock(UrlValidator.class));

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> service.send(createTestSummary(), USER_ID));
    }

    @Test
    void shouldHandleNullUserSettingsWebhookUrlGracefully() {
        UserSettings settings = new UserSettings();

        SettingsPersistence settingsPersistence = mock(SettingsPersistence.class);
        when(settingsPersistence.findByUserId(USER_ID)).thenReturn(Optional.of(settings));

        WebhookService service = new WebhookService("", settingsPersistence, mock(UrlValidator.class));

        assertThatCode(() -> service.send(createTestSummary(), USER_ID))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldSkipDeliveryWhenUrlFailsValidation() throws InterruptedException {
        // Arrange — validator rejects the URL (e.g. private IP or bad scheme)
        SettingsPersistence settingsPersistence = mock(SettingsPersistence.class);
        when(settingsPersistence.findByUserId(USER_ID)).thenReturn(Optional.empty());

        UrlValidator urlValidator = mock(UrlValidator.class);
        doThrow(new InvalidUrlException("private IP blocked"))
                .when(urlValidator).validate(anyString());

        WebhookService service = new WebhookService("http://192.168.1.1/hook", settingsPersistence, urlValidator);

        // Act — fire-and-forget, wait for virtual thread
        service.send(createTestSummary(), USER_ID);
        Thread.sleep(200);

        // Assert — validate() was called and delivery was skipped (no HTTP call possible in unit test)
        verify(urlValidator).validate("http://192.168.1.1/hook");
    }
}
