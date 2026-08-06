package com.example.backend.exception;

public class ValidationException extends DomainException {
    public ValidationException(String message) { super(message); }
}
