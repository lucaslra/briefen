package com.briefen.dto;

import com.briefen.model.Summary;

import java.time.Instant;

public record SummarizeResponse(
        String id,
        String url,
        String title,
        String summary,
        String modelUsed,
        Instant createdAt
) {
    public static SummarizeResponse from(Summary summary) {
        return new SummarizeResponse(
                summary.getId(),
                summary.getUrl(),
                summary.getTitle(),
                summary.getSummary(),
                summary.getModelUsed(),
                summary.getCreatedAt()
        );
    }
}
