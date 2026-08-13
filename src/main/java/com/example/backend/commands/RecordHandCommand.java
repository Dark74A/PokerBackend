package com.example.backend.commands;

import java.math.BigDecimal;
import java.util.Map;

public record RecordHandCommand(
    String sessionId,
    String hostId,
    Map<String, BigDecimal> deltas
) {}