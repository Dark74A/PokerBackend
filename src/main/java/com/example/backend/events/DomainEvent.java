package com.example.backend.events;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "event_store")
@CompoundIndex(
        name = "aggregate_version_idx",
        def = "{'aggregateId':1,'version':1}",
        unique = true
)
public class DomainEvent {

        @Id
        public String eventId;

        public String aggregateId;
        public String aggregateType;

        public long version;
        public Instant timestamp;
        public String eventType;

        public String userId;
        public Map<String, Object> payload;
        public Map<String, Object> metadata;
}