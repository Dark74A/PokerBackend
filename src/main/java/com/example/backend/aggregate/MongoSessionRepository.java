package com.example.backend.aggregate;

import com.example.backend.events.DomainEvent;
import com.example.backend.events.EventStore;
import com.example.backend.repositories.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

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
}