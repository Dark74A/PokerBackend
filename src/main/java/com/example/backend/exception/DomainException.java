package com.example.backend.exception;

public abstract class DomainException extends RuntimeException {
    protected DomainException(String message) { super(message); }
}

