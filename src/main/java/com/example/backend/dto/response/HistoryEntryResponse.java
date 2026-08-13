package com.example.backend.dto.response;

import java.time.Instant;

public record HistoryEntryResponse(
        String eventType,
        String description,
        Instant timestamp
) {}