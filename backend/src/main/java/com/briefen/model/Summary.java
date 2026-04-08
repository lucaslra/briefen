package com.briefen.model;

import java.time.Instant;

public class Summary {

    private String id;

    private String userId;

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

    // User-supplied annotation; null means no note has been added yet.
    private String notes;

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

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

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

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
