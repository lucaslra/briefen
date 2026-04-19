package com.briefen.dto;

import jakarta.validation.constraints.NotNull;

public record ReadStatusRequest(@NotNull Boolean isRead) {}
