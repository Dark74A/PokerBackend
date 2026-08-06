package com.example.backend.infrastructure;

import com.example.backend.events.DomainEvent;
import com.example.backend.exception.ConcurrencyException;
import com.example.backend.repositories.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TransactionalEventInserter {

    private final EventRepository eventRepository;

    @Transactional
    public List<DomainEvent> insertWithVersionCheck(
            String aggregateId,
            long expectedVersion,
            List<DomainEvent> newEvents) {

        if (newEvents.isEmpty()) {
            return List.of();
        }

        for (DomainEvent event : newEvents) {
            if (!aggregateId.equals(event.getAggregateId())) {
                throw new IllegalArgumentException(
                        "All events must belong to aggregate " + aggregateId
                );
            }
        }

        long currentVersion = eventRepository
                .findTopByAggregateIdOrderByVersionDesc(aggregateId)
                .map(DomainEvent::getVersion)
                .orElse(0L);

        if (currentVersion != expectedVersion) {
            throw new ConcurrencyException(
                    aggregateId,
                    expectedVersion,
                    currentVersion
            );
        }

        List<DomainEvent> eventsToInsert =
                new ArrayList<>(newEvents.size());

        long version = currentVersion;

        for (DomainEvent event : newEvents) {

            version++;

            eventsToInsert.add(
                    new DomainEvent(
                            event.getEventId(),
                            aggregateId,
                            event.getAggregateType(),
                            version,
                            event.getTimestamp(),
                            event.getEventType(),
                            event.getUserId(),
                            event.getPayload(),
                            event.getMetadata()
                    )
            );
        }

        eventRepository.insert(eventsToInsert);
        return eventsToInsert;
    }
}