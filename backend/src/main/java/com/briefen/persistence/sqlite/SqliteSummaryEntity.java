package com.briefen.persistence.sqlite;

import com.briefen.model.Summary;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "summaries")
public class SqliteSummaryEntity {

    @Id
    private String id;

    @Column(unique = true)
    private String url;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String summary;

    private String modelUsed;
    private Instant createdAt;

    @Column(nullable = false)
    private boolean isRead = false;

    private Instant savedAt;

    @Column(columnDefinition = "TEXT")
    private String notes;

    public SqliteSummaryEntity() {}

    public static SqliteSummaryEntity fromDomain(Summary s) {
        var entity = new SqliteSummaryEntity();
        entity.id = s.getId();
        entity.url = s.getUrl();
        entity.title = s.getTitle();
        entity.summary = s.getSummary();
        entity.modelUsed = s.getModelUsed();
        entity.createdAt = s.getCreatedAt();
        entity.isRead = s.getIsRead() != null && s.getIsRead();
        entity.savedAt = s.getSavedAt();
        entity.notes = s.getNotes();
        return entity;
    }

    public Summary toDomain() {
        var s = new Summary();
        s.setId(id);
        s.setUrl(url);
        s.setTitle(title);
        s.setSummary(summary);
        s.setModelUsed(modelUsed);
        s.setCreatedAt(createdAt);
        s.setIsRead(isRead);
        s.setSavedAt(savedAt);
        s.setNotes(notes);
        return s;
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

    public boolean isRead() { return isRead; }
    public void setRead(boolean isRead) { this.isRead = isRead; }

    public Instant getSavedAt() { return savedAt; }
    public void setSavedAt(Instant savedAt) { this.savedAt = savedAt; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
