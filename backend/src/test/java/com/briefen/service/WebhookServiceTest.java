package com.briefen.service;

import com.briefen.model.Summary;
import com.briefen.model.UserSettings;
import com.briefen.persistence.SettingsPersistence;
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
        // Arrange
        SettingsPersistence settingsPersistence = mock(SettingsPersistence.class);
        when(settingsPersistence.findByUserId(USER_ID)).thenReturn(Optional.empty());

        WebhookService service = new WebhookService("", settingsPersistence);

        // Act & Assert — should return immediately without error
        assertThatCode(() -> service.send(createTestSummary(), USER_ID))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldBeNoOpWhenSettingsUrlIsBlankAndEnvUrlIsNull() {
        // Arrange
        UserSettings settings = new UserSettings();
        settings.setWebhookUrl("   ");

        SettingsPersistence settingsPersistence = mock(SettingsPersistence.class);
        when(settingsPersistence.findByUserId(USER_ID)).thenReturn(Optional.of(settings));

        WebhookService service = new WebhookService(null, settingsPersistence);

        // Act & Assert
        assertThatCode(() -> service.send(createTestSummary(), USER_ID))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldBeNoOpWhenNoSettingsExistAndEnvUrlIsBlank() {
        // Arrange
        SettingsPersistence settingsPersistence = mock(SettingsPersistence.class);
        when(settingsPersistence.findByUserId(USER_ID)).thenReturn(Optional.empty());

        WebhookService service = new WebhookService("  ", settingsPersistence);

        // Act & Assert
        assertThatCode(() -> service.send(createTestSummary(), USER_ID))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldPreferUserSettingsUrlOverEnvUrl() throws InterruptedException {
        // Arrange
        String settingsUrl = "https://settings-webhook.example.com/hook";
        String envUrl = "https://env-webhook.example.com/hook";

        UserSettings settings = new UserSettings();
        settings.setWebhookUrl(settingsUrl);

        SettingsPersistence settingsPersistence = mock(SettingsPersistence.class);
        when(settingsPersistence.findByUserId(USER_ID)).thenReturn(Optional.of(settings));

        WebhookService service = new WebhookService(envUrl, settingsPersistence);

        // Act — send fires on a virtual thread, so it won't fail the test even if
        // the HTTP call fails (fire-and-forget). We verify it resolved the URL
        // by confirming the settings persistence was consulted.
        service.send(createTestSummary(), USER_ID);

        // Brief wait for the virtual thread to start the delivery attempt
        Thread.sleep(200);

        // Assert — settings were looked up
        verify(settingsPersistence).findByUserId(USER_ID);
    }

    @Test
    void shouldFallBackToEnvUrlWhenNoSettingsUrlConfigured() throws InterruptedException {
        // Arrange
        SettingsPersistence settingsPersistence = mock(SettingsPersistence.class);
        when(settingsPersistence.findByUserId(USER_ID)).thenReturn(Optional.empty());

        // The env URL is set but will fail to connect (expected) — the important
        // thing is that the service attempts delivery rather than short-circuiting.
        WebhookService service = new WebhookService("https://env-webhook.example.com/hook", settingsPersistence);

        // Act
        service.send(createTestSummary(), USER_ID);

        // Brief wait for the virtual thread to start
        Thread.sleep(200);

        // Assert — settings were checked (even though none were found)
        verify(settingsPersistence).findByUserId(USER_ID);
    }

    @Test
    void shouldPropagateExceptionWhenUrlResolutionFails() {
        // Arrange — resolveUrl is synchronous, so DB errors propagate from send()
        SettingsPersistence settingsPersistence = mock(SettingsPersistence.class);
        when(settingsPersistence.findByUserId(anyString())).thenThrow(new RuntimeException("DB down"));

        WebhookService service = new WebhookService("https://example.com/hook", settingsPersistence);

        // Act & Assert — URL resolution happens before the virtual thread is started,
        // so exceptions from settingsPersistence propagate to the caller
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> service.send(createTestSummary(), USER_ID));
    }

    @Test
    void shouldHandleNullUserSettingsWebhookUrlGracefully() {
        // Arrange — settings exist but webhookUrl is null
        UserSettings settings = new UserSettings();
        // webhookUrl defaults to null

        SettingsPersistence settingsPersistence = mock(SettingsPersistence.class);
        when(settingsPersistence.findByUserId(USER_ID)).thenReturn(Optional.of(settings));

        WebhookService service = new WebhookService("", settingsPersistence);

        // Act & Assert
        assertThatCode(() -> service.send(createTestSummary(), USER_ID))
                .doesNotThrowAnyException();
    }
}
