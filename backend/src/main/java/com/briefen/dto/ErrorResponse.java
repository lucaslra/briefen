package com.briefen.dto;

import java.time.Instant;

public record ErrorResponse(
        String error,
        int status,
        Instant timestamp
) {
    public static ErrorResponse of(String error, int status) {
        return new ErrorResponse(error, status, Instant.now());
    }
}
