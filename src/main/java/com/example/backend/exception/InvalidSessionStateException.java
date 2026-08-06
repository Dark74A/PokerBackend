package com.example.backend.exception;

public class InvalidSessionStateException extends DomainException {
    public InvalidSessionStateException(String message) { super(message); }
}
