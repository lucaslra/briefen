package com.briefen.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request to create a personal access token. */
public record CreateApiTokenRequest(
        @NotBlank @Size(max = 100) String name
) {}
