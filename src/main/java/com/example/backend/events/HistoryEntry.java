package com.example.backend.events;


import lombok.Builder;

import java.time.Instant;

@Builder
public class HistoryEntry {

    String id;
    String sessionId;
    String eventType;
    String description;
    String actorId;
    Instant timestamp;
    long version;


}
