package com.example.backend.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record SessionResponse(
        String sessionId,
        String sessionName,
        String hostId,
        String status,
        List<PlayerResponse> players,
        BigDecimal totalBuyIns,
        BigDecimal totalCashOuts
) {}

