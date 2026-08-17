package com.example.backend.infrastructure;

import com.example.backend.events.DomainEvent;
import com.example.backend.events.EventStore;
import com.example.backend.exception.ConcurrencyException;
import com.example.backend.repositories.EventRepository;
import com.mongodb.MongoException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MongoEventStore implements EventStore {

    private final TransactionalEventInserter inserter;
    private final EventRepository eventRepository;

    @Override
    public List<DomainEvent> append(
            String aggregateId,
            long expectedVersion,
            List<DomainEvent> newEvents) {

        try {

            return inserter.insertWithVersionCheck(
                    aggregateId,
                    expectedVersion,
                    newEvents
            );

        } catch (DataIntegrityViolationException ex) {

            boolean isConflict = ex instanceof DuplicateKeyException
                    || (ex.getCause() instanceof MongoException mongoEx
                    && mongoEx.hasErrorLabel("TransientTransactionError"));

            if (!isConflict) {
                throw ex;
            }

            long actualVersion = eventRepository
                    .findTopByAggregateIdOrderByVersionDesc(aggregateId)
                    .map(DomainEvent::getVersion)
                    .orElse(0L);

            throw new ConcurrencyException(aggregateId, expectedVersion, actualVersion);
        }
    }

    @Override
    public List<DomainEvent> loadEvents(String aggregateId) {
        return eventRepository
                .findByAggregateIdOrderByVersionAsc(aggregateId);
    }

    @Override
    public List<DomainEvent> loadEventsUntil(String aggregateId, Instant asOf) {
        return loadEvents(aggregateId).stream()
                .filter(e -> !e.getTimestamp().isAfter(asOf))
                .toList();
    }

    @Override
    public List<String> loadAllAggregateIds() {
        return eventRepository.findDistinctAggregateIds();
    }
}