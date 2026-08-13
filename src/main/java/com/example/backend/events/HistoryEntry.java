package com.example.backend.events;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "history_projections")
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
@Getter
public class HistoryEntry {

    @Id
    String id;
    String sessionId;
    String eventType;
    String description;
    String actorId;
    Instant timestamp;
    long version;


}
