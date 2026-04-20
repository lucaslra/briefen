package com.briefen.persistence.jpa;

import com.briefen.model.UserSettings;
import com.briefen.persistence.SettingsPersistence;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class JpaSettingsPersistenceTest {

    private static final String USER_ID = "settings-test-user";

    @Autowired
    private SettingsPersistence persistence;

    @Test
    void shouldReturnEmptyWhenUserHasNoSettings() {
        Optional<UserSettings> result = persistence.findByUserId("nonexistent-user");
        assertThat(result).isEmpty();
    }

    @Test
    void shouldSaveAndFindSettingsByUserId() {
        UserSettings settings = buildSettings(USER_ID);
        settings.setDefaultLength("shorter");
        settings.setOpenaiApiKey("sk-test");

        persistence.save(settings);
        Optional<UserSettings> found = persistence.findByUserId(USER_ID);

        assertThat(found).isPresent();
        assertThat(found.get().getDefaultLength()).isEqualTo("shorter");
        assertThat(found.get().getOpenaiApiKey()).isEqualTo("sk-test");
    }

    @Test
    void shouldUpdateExistingSettings() {
        UserSettings settings = buildSettings(USER_ID);
        settings.setDefaultLength("default");
        persistence.save(settings);

        UserSettings updated = buildSettings(USER_ID);
        updated.setDefaultLength("longer");
        updated.setWebhookUrl("https://example.com/hook");
        persistence.save(updated);

        Optional<UserSettings> found = persistence.findByUserId(USER_ID);
        assertThat(found).isPresent();
        assertThat(found.get().getDefaultLength()).isEqualTo("longer");
        assertThat(found.get().getWebhookUrl()).isEqualTo("https://example.com/hook");
    }

    @Test
    void shouldPersistAllOptionalFields() {
        UserSettings settings = buildSettings(USER_ID);
        settings.setDefaultLength("longer");
        settings.setModel("gpt-4o");
        settings.setNotificationsEnabled(true);
        settings.setOpenaiApiKey("sk-openai");
        settings.setAnthropicApiKey("sk-ant");
        settings.setReadeckApiKey("rk-key");
        settings.setReadeckUrl("https://readeck.example.com");
        settings.setWebhookUrl("https://webhook.example.com");
        settings.setCustomPrompt("Summarize briefly.");

        persistence.save(settings);
        UserSettings found = persistence.findByUserId(USER_ID).orElseThrow();

        assertThat(found.getModel()).isEqualTo("gpt-4o");
        assertThat(found.isNotificationsEnabled()).isTrue();
        assertThat(found.getAnthropicApiKey()).isEqualTo("sk-ant");
        assertThat(found.getReadeckApiKey()).isEqualTo("rk-key");
        assertThat(found.getReadeckUrl()).isEqualTo("https://readeck.example.com");
        assertThat(found.getCustomPrompt()).isEqualTo("Summarize briefly.");
    }

    @Test
    void shouldClearOptionalFieldsWhenSetToNull() {
        UserSettings initial = buildSettings(USER_ID);
        initial.setOpenaiApiKey("sk-old-key");
        persistence.save(initial);

        UserSettings cleared = buildSettings(USER_ID);
        cleared.setOpenaiApiKey(null);
        persistence.save(cleared);

        UserSettings found = persistence.findByUserId(USER_ID).orElseThrow();
        assertThat(found.getOpenaiApiKey()).isNull();
    }

    private UserSettings buildSettings(String userId) {
        var s = new UserSettings();
        s.setId(userId);
        return s;
    }
}
