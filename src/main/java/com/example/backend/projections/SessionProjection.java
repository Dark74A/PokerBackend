package com.example.backend.projections;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Document(collection = "session_projection")
@lombok.Builder
@lombok.Data
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class SessionProjection {
    @Id
    private String sessionId;
    private String sessionName;
    private String hostId;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;
    private long lastAppliedVersion;
    private List<PlayerProjection> players;
}

