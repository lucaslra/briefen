package com.briefen.dto;

import com.briefen.model.AuthSession;

import java.time.Instant;

/** A personal access token as shown in the management list — never includes token material. */
public record ApiTokenResponse(
        String id,
        String name,
        Instant createdAt,
        Instant lastUsedAt
) {
    public static ApiTokenResponse from(AuthSession s) {
        return new ApiTokenResponse(s.getId(), s.getName(), s.getCreatedAt(), s.getLastUsedAt());
    }
}
