package com.example.backend.exception;

public class UnauthorizedActionException extends DomainException {
    public UnauthorizedActionException(String message) { super(message); }
}
