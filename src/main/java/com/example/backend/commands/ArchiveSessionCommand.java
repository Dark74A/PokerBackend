package com.example.backend.commands;

public record ArchiveSessionCommand(
        String sessionId,
        String hostId
) {}