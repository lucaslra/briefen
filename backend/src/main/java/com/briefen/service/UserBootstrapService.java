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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Creates the initial admin user on first startup.
 *
 * Priority:
 *   1. BRIEFEN_AUTH_USERNAME + BRIEFEN_AUTH_PASSWORD env vars (explicit bootstrap)
 *   2. Generates a random password and logs it at WARN level (auto-bootstrap)
 *
 * Also migrates pre-multi-user data:
 *   - Legacy "default" settings row → copied to the admin's userId
 *   - Summaries with user_id=NULL → assigned to the admin user
 */
@Service
public class UserBootstrapService {

    private static final Logger log = LoggerFactory.getLogger(UserBootstrapService.class);

    /** ID of the legacy single-user settings row created before multi-user support. */
    private static final String LEGACY_SETTINGS_ID = "default";

    private final UserPersistence userPersistence;
    private final SettingsPersistence settingsPersistence;
    private final SummaryPersistence summaryPersistence;
    private final PasswordEncoder passwordEncoder;
    private final String configuredUsername;
    private final String configuredPassword;

    public UserBootstrapService(
            UserPersistence userPersistence,
            SettingsPersistence settingsPersistence,
            SummaryPersistence summaryPersistence,
            PasswordEncoder passwordEncoder,
            @Value("${briefen.auth.username:}") String configuredUsername,
            @Value("${briefen.auth.password:}") String configuredPassword) {
        this.userPersistence = userPersistence;
        this.settingsPersistence = settingsPersistence;
        this.summaryPersistence = summaryPersistence;
        this.passwordEncoder = passwordEncoder;
        this.configuredUsername = configuredUsername;
        this.configuredPassword = configuredPassword;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void bootstrap() {
        if (userPersistence.count() > 0) {
            log.debug("Users already exist — skipping bootstrap");
            return;
        }

        String username = (configuredUsername != null && !configuredUsername.isBlank())
                ? configuredUsername : "admin";
        String password;
        boolean generated = false;

        if (configuredPassword != null && !configuredPassword.isBlank()) {
            password = configuredPassword;
        } else {
            password = generateRandomPassword();
            generated = true;
        }

        if (generated) {
            String banner = """

=================================================================
Briefen — initial admin credentials
  Username : %s
  Password : %s
Set BRIEFEN_AUTH_USERNAME / BRIEFEN_AUTH_PASSWORD to use your own.
=================================================================
""".formatted(username, password);
            System.out.println(banner); // stdout — printed before DB write so it's never lost
            log.warn(banner);
        }

        String userId = UUID.randomUUID().toString();
        var user = new User(userId, username, passwordEncoder.encode(password), "ADMIN");
        user.setMainAdmin(true);
        userPersistence.save(user);

        if (!generated) {
            log.info("Created admin user '{}'", username);
        }

        migrateLegacyData(userId);
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

    private static String generateRandomPassword() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
