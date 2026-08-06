package com.example.backend.dto.request;

import jakarta.validation.constraints.NotNull;

public record RemovePlayerRequest(
        @NotNull String playerId
) {
}
