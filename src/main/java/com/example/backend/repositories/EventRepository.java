package com.example.backend.repositories;

import com.example.backend.events.DomainEvent;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends MongoRepository<DomainEvent, String> {

    List<DomainEvent> findByAggregateIdOrderByVersionAsc(
            String aggregateId
    );

    Optional<DomainEvent> findTopByAggregateIdOrderByVersionDesc(String aggregateId);

}