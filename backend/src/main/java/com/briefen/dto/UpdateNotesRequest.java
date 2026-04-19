package com.briefen.dto;

import jakarta.validation.constraints.Size;

public record UpdateNotesRequest(@Size(max = 10_000) String notes) {}
