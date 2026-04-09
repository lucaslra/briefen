package com.briefen.service;

import com.briefen.model.User;
import com.briefen.model.UserSettings;
import com.briefen.persistence.SettingsPersistence;
import com.briefen.persistence.SummaryPersistence;
import com.briefen.persistence.UserPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Runs on application startup to handle data migrations and API key seeding.
 *
 * The initial admin account is created through the browser-based first-run
 * setup flow ({@link SetupService}). This service handles:
 *   - Migrating pre-multi-user data (legacy settings row + orphaned summaries)
 *   - Seeding cloud LLM API keys from environment variables into the admin's settings
 *
 * Both tasks are idempotent and only take effect when relevant data exists.
 */
@Service
public class UserBootstrapService {

    private static final Logger log = LoggerFactory.getLogger(UserBootstrapService.class);

    /** ID of the legacy single-user settings row created before multi-user support. */
    private static final String LEGACY_SETTINGS_ID = "default";

    private final UserPersistence userPersistence;
    private final SettingsPersistence settingsPersistence;
    private final SummaryPersistence summaryPersistence;
    private final String openaiApiKey;
    private final String anthropicApiKey;

    public UserBootstrapService(
            UserPersistence userPersistence,
            SettingsPersistence settingsPersistence,
            SummaryPersistence summaryPersistence,
            @Value("${briefen.openai.api-key:}") String openaiApiKey,
            @Value("${briefen.anthropic.api-key:}") String anthropicApiKey) {
        this.userPersistence = userPersistence;
        this.settingsPersistence = settingsPersistence;
        this.summaryPersistence = summaryPersistence;
        this.openaiApiKey = openaiApiKey;
        this.anthropicApiKey = anthropicApiKey;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void bootstrap() {
        List<User> admins = userPersistence.findByRole("ADMIN");
        if (admins.isEmpty()) {
            log.info("No admin account found — waiting for first-run setup via the browser");
            return;
        }

        String adminUserId = admins.getFirst().getId();
        migrateLegacyData(adminUserId);
        seedApiKeys(adminUserId);
    }

    /**
     * Assigns pre-multi-user data to the bootstrapped admin.
     * The legacy "default" settings row (if present) is copied to the admin's userId.
     * Orphaned summaries (user_id=NULL) are assigned to the admin.
     */
    private void migrateLegacyData(String adminUserId) {
        settingsPersistence.findByUserId(LEGACY_SETTINGS_ID).ifPresent(legacy -> {
            // Copy legacy settings to admin's userId — old "default" row stays as orphan
            UserSettings adminSettings = copySettings(legacy, adminUserId);
            settingsPersistence.save(adminSettings);
            log.info("Migrated legacy settings to admin user '{}'", adminUserId);
        });

        long migrated = summaryPersistence.assignOrphanedSummaries(adminUserId);
        if (migrated > 0) {
            log.info("Migrated {} orphaned summaries to admin user '{}'", migrated, adminUserId);
        }
    }

    private static UserSettings copySettings(UserSettings source, String newId) {
        var target = new UserSettings();
        target.setId(newId);
        target.setDefaultLength(source.getDefaultLength());
        target.setModel(source.getModel());
        target.setNotificationsEnabled(source.isNotificationsEnabled());
        target.setOpenaiApiKey(source.getOpenaiApiKey());
        target.setAnthropicApiKey(source.getAnthropicApiKey());
        target.setReadeckApiKey(source.getReadeckApiKey());
        target.setReadeckUrl(source.getReadeckUrl());
        target.setWebhookUrl(source.getWebhookUrl());
        return target;
    }

    /**
     * Seeds OpenAI/Anthropic API keys from environment variables into the admin's settings.
     * Only runs when the env vars are set. Does not overwrite keys already present in the
     * database (e.g. from legacy migration or a previous seed).
     */
    private void seedApiKeys(String adminUserId) {
        if ((openaiApiKey == null || openaiApiKey.isBlank())
                && (anthropicApiKey == null || anthropicApiKey.isBlank())) {
            return;
        }

        UserSettings settings = settingsPersistence.findByUserId(adminUserId)
                .orElseGet(() -> {
                    var s = new UserSettings();
                    s.setId(adminUserId);
                    return s;
                });

        boolean changed = false;
        if (openaiApiKey != null && !openaiApiKey.isBlank()
                && (settings.getOpenaiApiKey() == null || settings.getOpenaiApiKey().isBlank())) {
            settings.setOpenaiApiKey(openaiApiKey);
            changed = true;
        }
        if (anthropicApiKey != null && !anthropicApiKey.isBlank()
                && (settings.getAnthropicApiKey() == null || settings.getAnthropicApiKey().isBlank())) {
            settings.setAnthropicApiKey(anthropicApiKey);
            changed = true;
        }
        if (changed) {
            settingsPersistence.save(settings);
            log.info("Seeded cloud LLM API keys from environment into admin settings");
        }
    }

}
