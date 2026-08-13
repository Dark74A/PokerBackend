package com.example.backend.aggregate;

import com.example.backend.events.DomainEvent;
import com.example.backend.events.EventStore;
import com.example.backend.exception.SessionNotFoundException;
import com.example.backend.repositories.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class MongoSessionRepository implements SessionRepository {

    private final EventStore eventStore;

    @Override
    public SessionAggregate load(String sessionId) {

        return SessionAggregate.rehydrate(
                eventStore.loadEvents(sessionId)
        );
    }

    @Override
    public List<DomainEvent> save(SessionAggregate aggregate) {

        return eventStore.append(
                aggregate.getId(),
                aggregate.getBaseVersion(),
                aggregate.getUncommittedEvents()
        );

    }

    public SessionAggregate loadAsOf(String sessionId, Instant asOf) {
        List<DomainEvent> events = eventStore.loadEventsUntil(sessionId, asOf);
        SessionAggregate aggregate = SessionAggregate.rehydrate(events);

        if (aggregate.getId() == null) {
            throw new SessionNotFoundException(sessionId);
        }

        return aggregate;
    }
}