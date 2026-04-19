package com.briefen.dto;

import jakarta.validation.constraints.Size;

/**
 * Accepts either a {@code url} to fetch and summarize, or raw {@code text}
 * (with an optional {@code title}) to summarize directly.
 * At least one of {@code url} or {@code text} must be provided —
 * validated in the controller / service layer rather than with annotations
 * because the constraint is cross-field.
 */
public record SummarizeRequest(
        @Size(max = 2048, message = "URL must not exceed 2048 characters.")
        String url,
        @Size(max = 500_000, message = "Text must not exceed 500,000 characters.")
        String text,
        @Size(max = 500, message = "Title must not exceed 500 characters.")
        String title,
        String lengthHint, // nullable — "shorter", "longer", or null for default
        String model,      // nullable — e.g. "gemma2:2b", "gemma3:4b"; null means use server default
        @Size(max = 2048, message = "Source URL must not exceed 2048 characters.")
        String sourceUrl   // nullable — original article URL for Readeck/pasted content attribution
) {}
