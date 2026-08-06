package com.example.backend.dto.response;

import java.time.Instant;

public record SessionSummaryResponse(
        String sessionId,
        String name,
        String status,
        Instant createdAt
) {}