package com.example.backend.events;

import java.time.Instant;
import java.util.List;

public interface EventStore {
    List<DomainEvent> append(String aggregateId, long expectedVersion, List<DomainEvent> newEvents);
    List<DomainEvent> loadEvents(String aggregateId);
    List<DomainEvent> loadEventsUntil(String aggregateId, Instant asOf);   // new
}