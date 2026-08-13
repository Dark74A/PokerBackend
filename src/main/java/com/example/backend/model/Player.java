package com.example.backend.model;

import java.math.BigDecimal;

public record Player(
        String playerId,
        String userId,
        String displayName,
        BigDecimal totalBuyIn,
        BigDecimal totalCashOut,
        BigDecimal chipStack,
        PlayerStatus status,
        String notes
) {}