package com.example.backend.events;

import java.util.Set;

public interface EventHandler {
    Set<String> supportedEventTypes();
    void handle(DomainEvent event);
}