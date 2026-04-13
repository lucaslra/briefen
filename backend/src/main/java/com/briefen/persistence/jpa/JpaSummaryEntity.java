package com.briefen.persistence.jpa;

import com.briefen.model.Summary;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "summaries",
        uniqueConstraints = @UniqueConstraint(columnNames = {"url", "user_id"}))
public class JpaSummaryEntity {

    @Id
    private String id;

    @Column(name = "user_id")
    private String userId;

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

    @Column(columnDefinition = "TEXT")
    private String tags;

    @Column(columnDefinition = "TEXT")
    private String articleText;

    public JpaSummaryEntity() {}

    public static JpaSummaryEntity fromDomain(Summary s) {
        var entity = new JpaSummaryEntity();
        entity.id = s.getId();
        entity.userId = s.getUserId();
        entity.url = s.getUrl();
        entity.title = s.getTitle();
        entity.summary = s.getSummary();
        entity.modelUsed = s.getModelUsed();
        entity.createdAt = s.getCreatedAt();
        entity.isRead = s.getIsRead() != null && s.getIsRead();
        entity.savedAt = s.getSavedAt();
        entity.notes = s.getNotes();
        entity.tags = tagsToString(s.getTags());
        entity.articleText = s.getArticleText();
        return entity;
    }

    public Summary toDomain() {
        var s = new Summary();
        s.setId(id);
        s.setUserId(userId);
        s.setUrl(url);
        s.setTitle(title);
        s.setSummary(summary);
        s.setModelUsed(modelUsed);
        s.setCreatedAt(createdAt);
        s.setIsRead(isRead);
        s.setSavedAt(savedAt);
        s.setNotes(notes);
        s.setTags(tagsFromString(tags));
        s.setArticleText(articleText);
        return s;
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

    public boolean isRead() { return isRead; }
    public void setRead(boolean isRead) { this.isRead = isRead; }

    public Instant getSavedAt() { return savedAt; }
    public void setSavedAt(Instant savedAt) { this.savedAt = savedAt; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public String getArticleText() { return articleText; }
    public void setArticleText(String articleText) { this.articleText = articleText; }

    private static String tagsToString(List<String> tags) {
        if (tags == null || tags.isEmpty()) return null;
        return String.join(",", tags);
    }

    private static List<String> tagsFromString(String tags) {
        if (tags == null || tags.isBlank()) return List.of();
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(t -> !t.isEmpty())
                .toList();
    }
}
