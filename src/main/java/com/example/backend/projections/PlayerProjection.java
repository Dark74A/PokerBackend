package com.example.backend.projections;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerProjection {

    private String playerId;

    private String displayName;

    @Builder.Default
    private BigDecimal totalBuyIn = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal totalCashOut = BigDecimal.ZERO;

    private String status;
}