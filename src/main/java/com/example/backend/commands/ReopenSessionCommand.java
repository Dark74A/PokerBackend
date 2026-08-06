package com.example.backend.commands;

public record ReopenSessionCommand(
        String sessionId,
        String hostId
) {}