package com.example.backend.repositories;

import com.example.backend.events.HistoryEntry;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface HistoryEntryRepository extends MongoRepository<HistoryEntry, String> {
    List<HistoryEntry> findBySessionIdOrderByVersionAsc(String sessionId);
}
