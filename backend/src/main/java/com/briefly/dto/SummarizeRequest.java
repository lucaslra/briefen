package com.briefly.dto;

import jakarta.validation.constraints.NotBlank;

public record SummarizeRequest(
        @NotBlank(message = "URL must not be blank")
        String url,
        String lengthHint // nullable — "shorter", "longer", or null for default
) {}
