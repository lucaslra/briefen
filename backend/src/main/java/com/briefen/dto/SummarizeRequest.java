package com.briefen.dto;

/**
 * Accepts either a {@code url} to fetch and summarize, or raw {@code text}
 * (with an optional {@code title}) to summarize directly.
 * At least one of {@code url} or {@code text} must be provided —
 * validated in the controller / service layer rather than with annotations
 * because the constraint is cross-field.
 */
public record SummarizeRequest(
        String url,
        String text,
        String title,
        String lengthHint, // nullable — "shorter", "longer", or null for default
        String model       // nullable — e.g. "gemma2:2b", "gemma3:4b"; null means use server default
) {}
