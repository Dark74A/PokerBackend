package com.example.backend.repositories;

import com.example.backend.aggregate.SessionAggregate;
import com.example.backend.events.DomainEvent;

import java.util.List;

public interface SessionRepository {

    SessionAggregate load(String sessionId);

    List<DomainEvent> save(SessionAggregate aggregate);

}