package com.briefen.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "summaries")
public class Summary {

    @Id
    private String id;

    @Indexed(unique = true, sparse = true)
    private String url;

    private String title;

    private String summary;
    private String modelUsed;
    private Instant createdAt;

    // Reading list fields. Legacy documents will have these as null.
    // isRead == null is treated as false (unread) by the service layer.
    // savedAt == null falls back to createdAt for display and sorting.
    private Boolean isRead;
    private Instant savedAt;

    public Summary() {}

    public Summary(String url, String title, String summary, String modelUsed) {
        this.url = url;
        this.title = title;
        this.summary = summary;
        this.modelUsed = modelUsed;
        this.createdAt = Instant.now();
        this.isRead = false;
        this.savedAt = this.createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getModelUsed() { return modelUsed; }
    public void setModelUsed(String modelUsed) { this.modelUsed = modelUsed; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean isRead) { this.isRead = isRead; }

    public Instant getSavedAt() { return savedAt; }
    public void setSavedAt(Instant savedAt) { this.savedAt = savedAt; }

    public boolean isEffectivelyRead() { return Boolean.TRUE.equals(isRead); }

    public Instant getEffectiveSavedAt() { return savedAt != null ? savedAt : createdAt; }
}
