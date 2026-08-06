package com.example.backend.exception;

public class SessionNotFoundException extends DomainException {
    public SessionNotFoundException(String sessionId) {
        super("Session not found: " + sessionId);
    }
}
