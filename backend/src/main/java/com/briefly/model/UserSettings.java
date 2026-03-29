package com.briefly.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

/**
 * Stores user preferences. Since Briefly has no auth, a single
 * document with a well-known id ("default") is used.
 */
@Document(collection = "settings")
public class UserSettings {

    public static final String DEFAULT_ID = "default";

    @Id
    private String id = DEFAULT_ID;

    private String defaultLength = "default"; // "shorter" | "default" | "longer"
    private String model; // null means use server default from application.yml
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

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
