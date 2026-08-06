package com.example.backend.exception;

public class PlayerNotFoundException extends DomainException {
    public PlayerNotFoundException(String playerId) {
        super("Player not found: " + playerId);
    }
}
