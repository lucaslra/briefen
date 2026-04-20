package com.briefen.service;

import com.briefen.model.User;
import com.briefen.model.UserSettings;
import com.briefen.persistence.SettingsPersistence;
import com.briefen.persistence.SummaryPersistence;
import com.briefen.persistence.UserPersistence;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserBootstrapServiceTest {

    private static final String ADMIN_ID = "admin-uuid";
    private static final String LEGACY_ID = "default";

    private UserBootstrapService service(String openaiKey, String anthropicKey,
                                         UserPersistence users,
                                         SettingsPersistence settings,
                                         SummaryPersistence summaries) {
        return new UserBootstrapService(users, settings, summaries, openaiKey, anthropicKey);
    }

    private User adminUser() {
        var u = new User(ADMIN_ID, "admin", "$2a$hash", "ADMIN");
        u.setCreatedAt(Instant.now());
        return u;
    }

    // ---- No admin → early return ----

    @Test
    void shouldDoNothingWhenNoAdminExists() {
        var users = mock(UserPersistence.class);
        when(users.findByRole("ADMIN")).thenReturn(List.of());
        var settings = mock(SettingsPersistence.class);
        var summaries = mock(SummaryPersistence.class);

        service("", "", users, settings, summaries).bootstrap();

        verifyNoInteractions(settings, summaries);
    }

    // ---- Legacy data migration ----

    @Test
    void shouldSkipMigrationWhenNoLegacySettingsExist() {
        var users = mock(UserPersistence.class);
        when(users.findByRole("ADMIN")).thenReturn(List.of(adminUser()));

        var settings = mock(SettingsPersistence.class);
        when(settings.findByUserId(LEGACY_ID)).thenReturn(Optional.empty());

        var summaries = mock(SummaryPersistence.class);
        when(summaries.assignOrphanedSummaries(ADMIN_ID)).thenReturn(0L);

        service("", "", users, settings, summaries).bootstrap();

        // save should never be called — no legacy data, no API keys to seed
        verify(settings, never()).save(any());
    }

    @Test
    void shouldCopyLegacySettingsToAdminUser() {
        var legacy = new UserSettings();
        legacy.setId(LEGACY_ID);
        legacy.setDefaultLength("shorter");
        legacy.setModel("gpt-4o");
        legacy.setOpenaiApiKey("sk-legacy-key");
        legacy.setWebhookUrl("https://hook.example.com");

        var users = mock(UserPersistence.class);
        when(users.findByRole("ADMIN")).thenReturn(List.of(adminUser()));

        var settings = mock(SettingsPersistence.class);
        when(settings.findByUserId(LEGACY_ID)).thenReturn(Optional.of(legacy));
        when(settings.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var summaries = mock(SummaryPersistence.class);
        when(summaries.assignOrphanedSummaries(ADMIN_ID)).thenReturn(0L);

        service("", "", users, settings, summaries).bootstrap();

        var captor = ArgumentCaptor.forClass(UserSettings.class);
        verify(settings, atLeastOnce()).save(captor.capture());
        UserSettings saved = captor.getAllValues().stream()
                .filter(s -> ADMIN_ID.equals(s.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(saved.getDefaultLength()).isEqualTo("shorter");
        assertThat(saved.getModel()).isEqualTo("gpt-4o");
        assertThat(saved.getOpenaiApiKey()).isEqualTo("sk-legacy-key");
        assertThat(saved.getWebhookUrl()).isEqualTo("https://hook.example.com");
    }

    @Test
    void shouldAssignOrphanedSummariesToAdmin() {
        var users = mock(UserPersistence.class);
        when(users.findByRole("ADMIN")).thenReturn(List.of(adminUser()));

        var settings = mock(SettingsPersistence.class);
        when(settings.findByUserId(anyString())).thenReturn(Optional.empty());

        var summaries = mock(SummaryPersistence.class);
        when(summaries.assignOrphanedSummaries(ADMIN_ID)).thenReturn(5L);

        service("", "", users, settings, summaries).bootstrap();

        verify(summaries).assignOrphanedSummaries(ADMIN_ID);
    }

    // ---- API key seeding ----

    @Test
    void shouldSkipSeedingWhenBothEnvKeysAreBlank() {
        var users = mock(UserPersistence.class);
        when(users.findByRole("ADMIN")).thenReturn(List.of(adminUser()));

        var settings = mock(SettingsPersistence.class);
        when(settings.findByUserId(anyString())).thenReturn(Optional.empty());

        var summaries = mock(SummaryPersistence.class);
        when(summaries.assignOrphanedSummaries(ADMIN_ID)).thenReturn(0L);

        service("  ", "  ", users, settings, summaries).bootstrap();

        // settings.save should not be called for seeding (no legacy data either)
        verify(settings, never()).save(any());
    }

    @Test
    void shouldSeedOpenAiKeyWhenSettingsHaveNoKey() {
        var users = mock(UserPersistence.class);
        when(users.findByRole("ADMIN")).thenReturn(List.of(adminUser()));

        var settings = mock(SettingsPersistence.class);
        when(settings.findByUserId(LEGACY_ID)).thenReturn(Optional.empty());
        when(settings.findByUserId(ADMIN_ID)).thenReturn(Optional.empty());
        when(settings.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var summaries = mock(SummaryPersistence.class);
        when(summaries.assignOrphanedSummaries(ADMIN_ID)).thenReturn(0L);

        service("sk-new-key", "", users, settings, summaries).bootstrap();

        var captor = ArgumentCaptor.forClass(UserSettings.class);
        verify(settings).save(captor.capture());
        assertThat(captor.getValue().getOpenaiApiKey()).isEqualTo("sk-new-key");
        assertThat(captor.getValue().getAnthropicApiKey()).isNull();
    }

    @Test
    void shouldSeedAnthropicKeyWhenSettingsHaveNoKey() {
        var users = mock(UserPersistence.class);
        when(users.findByRole("ADMIN")).thenReturn(List.of(adminUser()));

        var settings = mock(SettingsPersistence.class);
        when(settings.findByUserId(LEGACY_ID)).thenReturn(Optional.empty());
        when(settings.findByUserId(ADMIN_ID)).thenReturn(Optional.empty());
        when(settings.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var summaries = mock(SummaryPersistence.class);
        when(summaries.assignOrphanedSummaries(ADMIN_ID)).thenReturn(0L);

        service("", "sk-ant-key", users, settings, summaries).bootstrap();

        var captor = ArgumentCaptor.forClass(UserSettings.class);
        verify(settings).save(captor.capture());
        assertThat(captor.getValue().getAnthropicApiKey()).isEqualTo("sk-ant-key");
        assertThat(captor.getValue().getOpenaiApiKey()).isNull();
    }

    @Test
    void shouldNotOverwriteExistingOpenAiKey() {
        var existing = new UserSettings();
        existing.setId(ADMIN_ID);
        existing.setOpenaiApiKey("sk-already-set");

        var users = mock(UserPersistence.class);
        when(users.findByRole("ADMIN")).thenReturn(List.of(adminUser()));

        var settings = mock(SettingsPersistence.class);
        when(settings.findByUserId(LEGACY_ID)).thenReturn(Optional.empty());
        when(settings.findByUserId(ADMIN_ID)).thenReturn(Optional.of(existing));

        var summaries = mock(SummaryPersistence.class);
        when(summaries.assignOrphanedSummaries(ADMIN_ID)).thenReturn(0L);

        service("sk-new-from-env", "", users, settings, summaries).bootstrap();

        // No save — key was already present
        verify(settings, never()).save(any());
    }

    @Test
    void shouldSeedBothKeysWhenBothEnvKeysAreSet() {
        var users = mock(UserPersistence.class);
        when(users.findByRole("ADMIN")).thenReturn(List.of(adminUser()));

        var settings = mock(SettingsPersistence.class);
        when(settings.findByUserId(LEGACY_ID)).thenReturn(Optional.empty());
        when(settings.findByUserId(ADMIN_ID)).thenReturn(Optional.empty());
        when(settings.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var summaries = mock(SummaryPersistence.class);
        when(summaries.assignOrphanedSummaries(ADMIN_ID)).thenReturn(0L);

        service("sk-openai", "sk-ant", users, settings, summaries).bootstrap();

        var captor = ArgumentCaptor.forClass(UserSettings.class);
        verify(settings).save(captor.capture());
        assertThat(captor.getValue().getOpenaiApiKey()).isEqualTo("sk-openai");
        assertThat(captor.getValue().getAnthropicApiKey()).isEqualTo("sk-ant");
    }
}
