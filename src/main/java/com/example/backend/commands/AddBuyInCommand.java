package com.example.backend.commands;

import java.math.BigDecimal;

public record AddBuyInCommand(
        String sessionId,
        String hostId,
        String playerId,
        BigDecimal amount
) {}
