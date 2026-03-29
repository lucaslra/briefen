package com.briefen.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

/**
 * Stores user preferences. Since Briefen has no auth, a single
 * document with a well-known id ("default") is used.
 */
@Document(collection = "settings")
public class UserSettings {

    public static final String DEFAULT_ID = "default";

    @Id
    private String id = DEFAULT_ID;

    private String defaultLength = "default"; // "shorter" | "default" | "longer"
    private String model; // null means use server default from application.yml
    private boolean notificationsEnabled = false;
    private String openaiApiKey; // plaintext — acceptable for local single-user app
    private String readeckApiKey;
    private String readeckUrl;
    private Instant updatedAt;

    public UserSettings() {
        this.updatedAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDefaultLength() { return defaultLength; }
    public void setDefaultLength(String defaultLength) {
        this.defaultLength = defaultLength;
        this.updatedAt = Instant.now();
    }

    public String getModel() { return model; }
    public void setModel(String model) {
        this.model = model;
        this.updatedAt = Instant.now();
    }

    public String getOpenaiApiKey() { return openaiApiKey; }
    public void setOpenaiApiKey(String openaiApiKey) {
        this.openaiApiKey = openaiApiKey;
        this.updatedAt = Instant.now();
    }

    public boolean isNotificationsEnabled() { return notificationsEnabled; }
    public void setNotificationsEnabled(boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
        this.updatedAt = Instant.now();
    }

    public String getReadeckApiKey() { return readeckApiKey; }
    public void setReadeckApiKey(String readeckApiKey) {
        this.readeckApiKey = readeckApiKey;
        this.updatedAt = Instant.now();
    }

    public String getReadeckUrl() { return readeckUrl; }
    public void setReadeckUrl(String readeckUrl) {
        this.readeckUrl = readeckUrl;
        this.updatedAt = Instant.now();
    }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
