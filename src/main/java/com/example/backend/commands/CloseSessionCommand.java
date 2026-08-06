package com.example.backend.commands;

public record CloseSessionCommand(
        String sessionId,
        String hostId
) {}