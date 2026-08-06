package com.example.backend.dto.response;

public record AuthResponse(
        String token,
        String userId,
        String username
) {}