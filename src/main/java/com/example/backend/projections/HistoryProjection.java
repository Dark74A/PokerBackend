package com.example.backend.projections;

import com.example.backend.events.DomainEvent;
import com.example.backend.events.EventHandler;
import com.example.backend.events.EventType;
import com.example.backend.events.HistoryEntry;
import com.example.backend.repositories.HistoryEntryRepository;
import com.example.backend.repositories.SessionProjectionRepository;
import org.springframework.dao.DuplicateKeyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class HistoryProjection implements EventHandler {

    private final HistoryEntryRepository historyEntryRepository;
    private final SessionProjectionRepository sessionProjectionRepository;

    @Override
    public Set<String> supportedEventTypes() {
        return Set.of(
                EventType.SESSION_CREATED,
                EventType.PLAYER_ADDED,
                EventType.PLAYER_REMOVED,
                EventType.BUY_IN_ADDED,
                EventType.CASH_OUT_ADDED,
                EventType.SESSION_CLOSED,
                EventType.SESSION_ARCHIVED,
                EventType.SESSION_REOPENED
        );
    }

    @Override
    public void handle(DomainEvent event) {
        String description = buildDescription(event);

        HistoryEntry entry = HistoryEntry.builder()
                .id(event.getEventId())
                .sessionId(event.getAggregateId())
                .eventType(event.getEventType())
                .description(description)
                .actorId(event.getUserId())
                .timestamp(event.getTimestamp())
                .version(event.getVersion())
                .build();

        try {
            historyEntryRepository.save(entry);
        } catch (DuplicateKeyException ex) {
            log.info("History entry for event {} already recorded, skipping", event.getEventId());
        }
    }

    private String buildDescription(DomainEvent event) {
        Map<String, Object> payload = event.getPayload();

        return switch (event.getEventType()) {
            case EventType.SESSION_CREATED ->
                    "Session \"" + payload.get("name") + "\" created";

            case EventType.PLAYER_ADDED ->
                    payload.get("displayName") + " joined the session";

            case EventType.PLAYER_REMOVED ->
                    resolvePlayerName(event.getAggregateId(), (String) payload.get("playerId")) + " was removed";

            case EventType.BUY_IN_ADDED ->
                    resolvePlayerName(event.getAggregateId(), (String) payload.get("playerId"))
                            + " bought in for ₹" + payload.get("amount");

            case EventType.CASH_OUT_ADDED ->
                    resolvePlayerName(event.getAggregateId(), (String) payload.get("playerId"))
                            + " cashed out ₹" + payload.get("amount");

            case EventType.SESSION_CLOSED -> "Session closed";
            case EventType.SESSION_ARCHIVED -> "Session archived";
            case EventType.SESSION_REOPENED -> "Session reopened";

            default -> "Unknown event: " + event.getEventType();
        };
    }

    private String resolvePlayerName(String sessionId, String playerId) {
        return sessionProjectionRepository.findById(sessionId)
                .flatMap(session -> session.getPlayers().stream()
                        .filter(p -> p.getPlayerId().equals(playerId))
                        .findFirst())
                .map(PlayerProjection::getDisplayName)
                .orElse("Unknown player");
    }
}