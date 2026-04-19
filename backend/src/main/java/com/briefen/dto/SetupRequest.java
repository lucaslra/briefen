package com.briefen.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SetupRequest(
        @NotBlank @Size(min = 3, max = 255) String username,
        @NotBlank String password
) {}
