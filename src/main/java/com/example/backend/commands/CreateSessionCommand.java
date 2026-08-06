package com.example.backend.commands;

public record CreateSessionCommand(

        String sessionId,
        String hostId,
        String name,
        String description
){}
