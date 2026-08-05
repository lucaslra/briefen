package com.briefen.dto;

import com.briefen.model.AuthSession;

import java.time.Instant;

/** Response for a freshly created token — includes the plaintext exactly once. */
public record CreatedApiTokenResponse(
        String id,
        String name,
        String token,
        Instant createdAt
) {
    public static CreatedApiTokenResponse from(AuthSession s, String plaintext) {
        return new CreatedApiTokenResponse(s.getId(), s.getName(), plaintext, s.getCreatedAt());
    }
}
