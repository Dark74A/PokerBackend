package com.example.backend.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record AddBuyInRequest(
        String playerId,

        @Positive
        @DecimalMin("0.01")
        BigDecimal amount
) {
}
