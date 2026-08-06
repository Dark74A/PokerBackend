package com.example.backend.dto.response;

import java.math.BigDecimal;

public record PlayerResponse(
        String playerId,
        String displayName,
        BigDecimal totalBuyIn,
        BigDecimal totalCashOut,
        BigDecimal profitLoss,
        String status
) {}
