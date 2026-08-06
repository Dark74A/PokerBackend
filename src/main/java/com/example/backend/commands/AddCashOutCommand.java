package com.example.backend.commands;

import java.math.BigDecimal;

public record AddCashOutCommand(
        String sessionId,
        String hostId,
        String playerId,
        BigDecimal amount
) {}
