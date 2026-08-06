package com.example.backend.helpers;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UuidGenerator implements IdGenerator {

    public String nextId() {
        return UUID.randomUUID().toString();
    }
}