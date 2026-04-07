package com.briefen.persistence.sqlite;

import com.briefen.model.UserSettings;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "settings")
public class SqliteUserSettingsEntity {

    @Id
    private String id = UserSettings.DEFAULT_ID;

    private String defaultLength = "default";
    private String model;
    private boolean notificationsEnabled = false;
    private String openaiApiKey;
    @Column(name = "anthropic_api_key")
    private String anthropicApiKey;
    private String readeckApiKey;
    private String readeckUrl;
    @Column(name = "webhook_url")
    private String webhookUrl;
    private Instant updatedAt;

    public SqliteUserSettingsEntity() {
        this.updatedAt = Instant.now();
    }

    public static SqliteUserSettingsEntity fromDomain(UserSettings s) {
        var entity = new SqliteUserSettingsEntity();
        entity.id = s.getId();
        entity.defaultLength = s.getDefaultLength();
        entity.model = s.getModel();
        entity.notificationsEnabled = s.isNotificationsEnabled();
        entity.openaiApiKey = s.getOpenaiApiKey();
        entity.anthropicApiKey = s.getAnthropicApiKey();
        entity.readeckApiKey = s.getReadeckApiKey();
        entity.readeckUrl = s.getReadeckUrl();
        entity.webhookUrl = s.getWebhookUrl();
        entity.updatedAt = s.getUpdatedAt();
        return entity;
    }

    public UserSettings toDomain() {
        var s = new UserSettings();
        s.setId(id);
        s.setUpdatedAt(updatedAt);
        s.setDefaultLength(defaultLength);
        s.setModel(model);
        s.setNotificationsEnabled(notificationsEnabled);
        s.setOpenaiApiKey(openaiApiKey);
        s.setAnthropicApiKey(anthropicApiKey);
        s.setReadeckApiKey(readeckApiKey);
        s.setReadeckUrl(readeckUrl);
        s.setWebhookUrl(webhookUrl);
        s.setUpdatedAt(updatedAt); // Re-set to original value after setters updated it
        return s;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDefaultLength() { return defaultLength; }
    public void setDefaultLength(String defaultLength) { this.defaultLength = defaultLength; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public boolean isNotificationsEnabled() { return notificationsEnabled; }
    public void setNotificationsEnabled(boolean notificationsEnabled) { this.notificationsEnabled = notificationsEnabled; }

    public String getOpenaiApiKey() { return openaiApiKey; }
    public void setOpenaiApiKey(String openaiApiKey) { this.openaiApiKey = openaiApiKey; }

    public String getAnthropicApiKey() { return anthropicApiKey; }
    public void setAnthropicApiKey(String anthropicApiKey) { this.anthropicApiKey = anthropicApiKey; }

    public String getReadeckApiKey() { return readeckApiKey; }
    public void setReadeckApiKey(String readeckApiKey) { this.readeckApiKey = readeckApiKey; }

    public String getReadeckUrl() { return readeckUrl; }
    public void setReadeckUrl(String readeckUrl) { this.readeckUrl = readeckUrl; }

    public String getWebhookUrl() { return webhookUrl; }
    public void setWebhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
