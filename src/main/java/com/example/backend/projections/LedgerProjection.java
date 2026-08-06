package com.example.backend.projections;

import com.example.backend.events.DomainEvent;
import com.example.backend.events.EventHandler;
import com.example.backend.events.EventType;
import com.example.backend.model.PlayerStatus;
import com.example.backend.model.SessionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.Decimal128;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class LedgerProjection implements EventHandler {

    private final MongoTemplate mongoTemplate;

    private static BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        if (value instanceof Decimal128 d128) {
            return d128.bigDecimalValue();
        }
        if (value == null) {
            return null;
        }
        throw new IllegalStateException("Unexpected type for BigDecimal field: " + value.getClass());
    }

    @Override
    public Set<String> supportedEventTypes() {
        return Set.of(
                EventType.SESSION_CREATED,
                EventType.PLAYER_ADDED,
                EventType.BUY_IN_ADDED,
                EventType.PLAYER_REMOVED
        );
    }

    @Override
    public void handle(DomainEvent event) {
        switch (event.getEventType()) {
            case EventType.SESSION_CREATED -> handleSessionCreated(event);
            case EventType.PLAYER_ADDED -> handlePlayerAdded(event);
            case EventType.BUY_IN_ADDED -> handleBuyInAdded(event);
            case EventType.CASH_OUT_ADDED -> handleCashOutAdded(event);
            case EventType.PLAYER_REMOVED -> handlePlayerRemoved(event);
            default -> throw new IllegalStateException(
                    "Unsupported event: " + event.getEventType());
        }
    }

    private void handleSessionCreated(DomainEvent event) {
        Map<String, Object> payload = event.getPayload();

        Query query = Query.query(Criteria.where("_id").is(event.getAggregateId()));

        Update update = new Update()
                .setOnInsert("sessionId", event.getAggregateId())
                .setOnInsert("sessionName", payload.get("name"))
                .setOnInsert("hostId", payload.get("hostId"))
                .setOnInsert("status", SessionStatus.ACTIVE.name())
                .setOnInsert("createdAt", event.getTimestamp())
                .setOnInsert("updatedAt", event.getTimestamp())
                .setOnInsert("lastAppliedVersion", event.getVersion())
                .setOnInsert("players", java.util.List.of());

        var result = mongoTemplate.upsert(query, update, SessionProjection.class);

        if (result.getUpsertedId() == null && result.getModifiedCount() == 0) {
            log.info("SessionCreated for {} already applied, skipping", event.getAggregateId());
        }
    }

    private void handlePlayerAdded(DomainEvent event) {
        Map<String, Object> payload = event.getPayload();

        PlayerProjection player = PlayerProjection.builder()
                .playerId((String) payload.get("playerId"))
                .displayName((String) payload.get("displayName"))
                .totalBuyIn(BigDecimal.ZERO)
                .totalCashOut(BigDecimal.ZERO)
                .status(PlayerStatus.ACTIVE.name())
                .build();

        Query query = Query.query(
                Criteria.where("_id").is(event.getAggregateId())
                        .and("lastAppliedVersion").lt(event.getVersion())
        );

        Update update = new Update()
                .push("players", player)
                .set("updatedAt", Instant.now())
                .set("lastAppliedVersion", event.getVersion());

        var result = mongoTemplate.updateFirst(query, update, SessionProjection.class);

        if (result.getModifiedCount() == 0) {
            log.warn(
                    "PlayerAdded version {} for session {} not applied — " +
                            "either already processed, or projection missing (out-of-order delivery)",
                    event.getVersion(), event.getAggregateId()
            );
        }
    }

    private void handleBuyInAdded(DomainEvent event) {

        Map<String, Object> payload = event.getPayload();
        String playerId = (String) payload.get("playerId");
        BigDecimal amount = toBigDecimal(payload.get("amount"));

        Query query = Query.query(
                Criteria.where("_id").is(event.getAggregateId())
                        .and("lastAppliedVersion").lt(event.getVersion())
                        .and("players.playerId").is(playerId)
        );

        Update update = new Update()
                .inc("players.$.totalBuyIn", amount)
                .set("updatedAt", event.getTimestamp())
                .set("lastAppliedVersion", event.getVersion());

        var result =
                mongoTemplate.updateFirst(
                        query,
                        update,
                        SessionProjection.class
                );

        if (result.getModifiedCount() == 0) {

            log.warn(
                    "BUY_IN_ADDED version {} for session {} ignored. " +
                            "Already processed, player missing, or event out of order.",
                    event.getVersion(),
                    event.getAggregateId()
            );
        }
    }

    private void handleCashOutAdded(DomainEvent event) {

        Map<String, Object> payload = event.getPayload();
        String playerId = (String) payload.get("playerId");
        BigDecimal amount = toBigDecimal(payload.get("amount"));

        Query query = Query.query(
                Criteria.where("_id").is(event.getAggregateId())
                        .and("lastAppliedVersion").lt(event.getVersion())
                        .and("players.playerId").is(playerId)
        );

        Update update = new Update()
                .inc("players.$.totalCashOut", amount)
                .set("updatedAt", event.getTimestamp())
                .set("lastAppliedVersion", event.getVersion());

        var result =
                mongoTemplate.updateFirst(
                        query,
                        update,
                        SessionProjection.class
                );

        if (result.getModifiedCount() == 0) {

            log.warn(
                    "CASH_OUT_ADDED version {} for session {} ignored. " +
                            "Already processed, player missing, or event out of order.",
                    event.getVersion(),
                    event.getAggregateId()
            );
        }
    }

    private void handlePlayerRemoved(DomainEvent event) {

        Map<String, Object> payload = event.getPayload();
        String playerId = (String) payload.get("playerId");

        Query query = Query.query(
                Criteria.where("_id").is(event.getAggregateId())
                        .and("lastAppliedVersion").lt(event.getVersion())
                        .and("players.playerId").is(playerId)
        );

        Update update = new Update()
                .set("players.$.status", PlayerStatus.INACTIVE.name())
                .set("updatedAt", event.getTimestamp())
                .set("lastAppliedVersion", event.getVersion());

        var result = mongoTemplate.updateFirst(query, update, SessionProjection.class);

        if (result.getModifiedCount() == 0) {
            log.warn(
                    "PlayerRemoved version {} for session {} not applied — " +
                            "already processed, player missing, or out-of-order delivery",
                    event.getVersion(), event.getAggregateId()
            );
        }
    }
}