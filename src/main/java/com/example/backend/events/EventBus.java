package com.example.backend.events;

import java.util.List;

public interface EventBus {
    void publish(List<DomainEvent> events);
    void subscribe(EventHandler handler);
}