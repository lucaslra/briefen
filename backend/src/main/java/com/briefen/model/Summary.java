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

    public Summary() {}

    public Summary(String url, String title, String summary, String modelUsed) {
        this.url = url;
        this.title = title;
        this.summary = summary;
        this.modelUsed = modelUsed;
        this.createdAt = Instant.now();
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
}
