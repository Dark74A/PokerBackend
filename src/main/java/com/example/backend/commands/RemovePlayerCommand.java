package com.example.backend.commands;

public record RemovePlayerCommand(
   String sessionId,
   String hostId,
   String playerId
) {}
