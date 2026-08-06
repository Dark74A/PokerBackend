package com.example.backend.commands;


public record AddPlayerCommand(
        String sessionId,
        String hostId,
        String linkedUserId,
        String displayName
) {}