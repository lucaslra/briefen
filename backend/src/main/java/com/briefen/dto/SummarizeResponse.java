package com.briefen.dto;

import com.briefen.model.Summary;

import java.time.Instant;
import java.util.List;

public record SummarizeResponse(
        String id,
        String url,
        String title,
        String summary,
        String modelUsed,
        Instant createdAt,
        Boolean isRead,
        Instant savedAt,
        String notes,
        List<String> tags,
        boolean hasArticleText
) {
    public static SummarizeResponse from(Summary summary) {
        return new SummarizeResponse(
                summary.getId(),
                summary.getUrl(),
                summary.getTitle(),
                summary.getSummary(),
                summary.getModelUsed(),
                summary.getCreatedAt(),
                summary.isEffectivelyRead(),
                summary.getEffectiveSavedAt(),
                summary.getNotes(),
                summary.getTags() != null ? summary.getTags() : List.of(),
                summary.getArticleText() != null && !summary.getArticleText().isEmpty()
        );
    }
}
