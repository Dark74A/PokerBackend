package com.example.backend.exception;

public class InvalidCredentialsException extends DomainException {
    public InvalidCredentialsException(String message) { super(message); }
}