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
        String url,
        @Size(max = 500_000, message = "Text must not exceed 500,000 characters.")
        String text,
        String title,
        String lengthHint, // nullable — "shorter", "longer", or null for default
        String model,      // nullable — e.g. "gemma2:2b", "gemma3:4b"; null means use server default
        String sourceUrl   // nullable — original article URL for Readeck/pasted content attribution
) {}
