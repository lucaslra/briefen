package com.briefen.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateTagsRequest(@NotNull List<@Size(max = 50) String> tags) {}
