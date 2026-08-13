package com.example.backend.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.math.BigDecimal;
import java.util.Map;

public record RecordHandRequest(
        @NotEmpty(message = "A hand needs at least one player delta")
        Map<String, BigDecimal> deltas
) {}