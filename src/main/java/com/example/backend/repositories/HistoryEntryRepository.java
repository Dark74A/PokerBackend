package com.example.backend.repositories;

import com.example.backend.events.HistoryEntry;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface HistoryEntryRepository extends MongoRepository<HistoryEntry, String> {
}
