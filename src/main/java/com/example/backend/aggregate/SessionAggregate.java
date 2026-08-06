package com.example.backend.aggregate;

import com.example.backend.commands.*;
import com.example.backend.events.DomainEvent;
import com.example.backend.events.EventType;
import com.example.backend.exception.*;
import com.example.backend.model.Player;
import com.example.backend.model.PlayerStatus;
import com.example.backend.model.SessionStatus;
import lombok.Getter;
import org.bson.types.Decimal128;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static com.example.backend.events.EventType.*;

@Getter
public class SessionAggregate {

    private String id;
    private SessionStatus status;
    private String hostId;
    private String name;
    private String description;

    private Map<String, Player> players = new HashMap<>();
    private long version;
    private long baseVersion;
    private final List<DomainEvent> uncommittedEvents = new ArrayList<>();

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

    private static final Map<SessionStatus, Set<SessionStatus>> ALLOWED_TRANSITIONS = Map.of(
            SessionStatus.ACTIVE, Set.of(
                    SessionStatus.CLOSED,
                    SessionStatus.ARCHIVED
            ),
            SessionStatus.CLOSED, Set.of(
                    SessionStatus.ACTIVE
            ),
            SessionStatus.ARCHIVED, Set.of(
                    SessionStatus.ACTIVE
            )
    );

    private void requireTransitionAllowed(SessionStatus target) {
        if (!ALLOWED_TRANSITIONS.getOrDefault(status, Set.of()).contains(target)) {
            throw new InvalidSessionStateException(
                    "Cannot transition from " + status + " to " + target
            );
        }
    }

    public static SessionAggregate rehydrate(List<DomainEvent> history) {
        SessionAggregate aggregate = new SessionAggregate();
        for (DomainEvent event : history) {
            aggregate.apply(event);
        }
        aggregate.baseVersion = aggregate.version;

        return aggregate;
    }

    public void handle(CreateSessionCommand cmd) {

        if (id != null) {
            throw new InvalidSessionStateException("Session Already Exists");
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("hostId", cmd.hostId());
        payload.put("name", cmd.name());
        payload.put("description", cmd.description());
        raise(
                new DomainEvent(
                        UUID.randomUUID().toString(),
                        cmd.sessionId(),
                        "SESSION",
                        0,
                        Instant.now(),
                        SESSION_CREATED,
                        cmd.hostId(),
                        payload,
                        Map.of()
                )
        );
    }

    public String handle(AddPlayerCommand cmd) {

        if (id == null) {
            throw new SessionNotFoundException("Session does not exist.");
        }

        if (!hostId.equals(cmd.hostId())) {
            throw new UnauthorizedActionException("You are not the host");
        }

        if (status == SessionStatus.CLOSED) {
            throw new InvalidSessionStateException("Session is closed.");
        }

        if (cmd.linkedUserId() != null) {
            boolean alreadyInSession = players.values().stream()
                    .anyMatch(p -> cmd.linkedUserId().equals(p.playerId()));
            if (alreadyInSession) {
                throw new InvalidSessionStateException("This user is already a player in this session.");
            }
        }

        String playerId = UUID.randomUUID().toString();

        Map<String, Object> payload = new HashMap<>();
        payload.put("playerId", playerId);
        payload.put("linkedUserId", cmd.linkedUserId());
        payload.put("displayName", cmd.displayName());

        raise(
                new DomainEvent(
                        UUID.randomUUID().toString(),
                        id,
                        "SESSION",
                        version,
                        Instant.now(),
                        PLAYER_ADDED,
                        cmd.hostId(),
                        payload,
                        Map.of()
                )
        );

        return playerId;
    }

    public String handle(AddBuyInCommand cmd) {

        if (id == null) {
            throw new SessionNotFoundException("Session does not exist.");
        }

        if (!hostId.equals(cmd.hostId())) {
            throw new UnauthorizedActionException("You are not the host");
        }

        if (status == SessionStatus.CLOSED) {
            throw new InvalidSessionStateException("Session is closed.");
        }

        Player player = players.get(cmd.playerId());

        if (player == null) {
            throw new PlayerNotFoundException("Player " + cmd.playerId() + " not found in this session.");
        }

        if (player.status() == PlayerStatus.INACTIVE) {
            throw new InvalidSessionStateException(
                    "Player has been removed from this session."
            );
        }

        if (cmd.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Buy-in must be positive.");
        }



        Map<String, Object> payload = new HashMap<>();
        String buyInId = UUID.randomUUID().toString();

        payload.put("buyInId", buyInId);
        payload.put("playerId", cmd.playerId());
        payload.put("amount", cmd.amount());

        raise(
                new DomainEvent(
                        UUID.randomUUID().toString(),
                        id,
                        "SESSION",
                        0,
                        Instant.now(),
                        EventType.BUY_IN_ADDED,
                        cmd.hostId(),
                        payload,
                        Map.of()
                )
        );

        return buyInId;
    }

    public String handle(AddCashOutCommand cmd) {
        if (id == null) {
            throw new SessionNotFoundException("Session does not exist.");
        }

        if (!hostId.equals(cmd.hostId())) {
            throw new UnauthorizedActionException("You are not the host");
        }

        if (status == SessionStatus.CLOSED) {
            throw new InvalidSessionStateException("Session is closed.");
        }

        Player player = players.get(cmd.playerId());

        if (player == null) {
            throw new PlayerNotFoundException("Player " + cmd.playerId() + " not found in this session.");
        }

        if (player.status() == PlayerStatus.INACTIVE) {
            throw new InvalidSessionStateException(
                    "Player has been removed from this session."
            );
        }

        if (cmd.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Cashout must be positive.");
        }

        Map<String, Object> payload = new HashMap<>();
        String cashOutId = UUID.randomUUID().toString();

        payload.put("cashOutId", cashOutId);
        payload.put("playerId", cmd.playerId());
        payload.put("amount", cmd.amount());

        raise(
                new DomainEvent(
                        UUID.randomUUID().toString(),
                        id,
                        "SESSION",
                        0,
                        Instant.now(),
                        EventType.CASH_OUT_ADDED,
                        cmd.hostId(),
                        payload,
                        Map.of()
                )
        );

        return cashOutId;
    }

    public void handle(CloseSessionCommand cmd) {

        if (id == null) {
            throw new SessionNotFoundException("Session does not exist.");
        }

        if (!hostId.equals(cmd.hostId())) {
            throw new UnauthorizedActionException("Only the host can close the session.");
        }
        requireTransitionAllowed(SessionStatus.CLOSED);

        Map<String, Object> payload = Map.of("previousStatus", status.name());
        raise(
                new DomainEvent(
                        UUID.randomUUID().toString(),
                        id,
                        "SESSION",
                        0,
                        Instant.now(),
                        EventType.SESSION_CLOSED,
                        cmd.hostId(),
                        payload,
                        Map.of()
                )
        );

    }

    public void handle(ArchiveSessionCommand cmd) {

        if (id == null) {
            throw new SessionNotFoundException("Session does not exist.");
        }

        if (!hostId.equals(cmd.hostId())) {
            throw new UnauthorizedActionException("Only the host can archive the session.");
        }

        requireTransitionAllowed(SessionStatus.ARCHIVED);

        Map<String, Object> payload = Map.of("previousStatus", status.name());
        raise(
                new DomainEvent(
                        UUID.randomUUID().toString(),
                        id,
                        "SESSION",
                        0,
                        Instant.now(),
                        EventType.SESSION_ARCHIVED,
                        cmd.hostId(),
                        payload,
                        Map.of()
                )
        );

    }

    public void handle(ReopenSessionCommand cmd) {

        if (id == null) {
            throw new SessionNotFoundException("Session does not exist.");
        }

        if (!hostId.equals(cmd.hostId())) {
            throw new UnauthorizedActionException("Only the host can reopen the session.");
        }

        requireTransitionAllowed(SessionStatus.ACTIVE);

        Map<String, Object> payload = Map.of("previousStatus", status.name());
        raise(
                new DomainEvent(
                        UUID.randomUUID().toString(),
                        id,
                        "SESSION",
                        0,
                        Instant.now(),
                        EventType.SESSION_REOPENED,
                        cmd.hostId(),
                        payload,
                        Map.of()
                )
        );

    }

    public void handle(RemovePlayerCommand cmd) {

        if (id == null) {
            throw new SessionNotFoundException("Session does not exist.");
        }

        if (!hostId.equals(cmd.hostId())) {
            throw new UnauthorizedActionException("Only the host can remove a player.");
        }

        Player player = players.get(cmd.playerId());
        if (player == null) {
            throw new PlayerNotFoundException("Player " + cmd.playerId() + " not found in this session.");
        }

        if (player.status() == PlayerStatus.INACTIVE) {
            throw new InvalidSessionStateException("Player has already been removed from this session.");
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("playerId", cmd.playerId());

        raise(
                new DomainEvent(
                        UUID.randomUUID().toString(),
                        id,
                        "SESSION",
                        0,
                        Instant.now(),
                        EventType.PLAYER_REMOVED,
                        cmd.hostId(),
                        payload,
                        Map.of()
                )
        );
    }

    private void raise(DomainEvent event) {
        apply(event);
        uncommittedEvents.add(event);
    }

    private void apply(DomainEvent event) {

        switch (event.getEventType()) {

            case SESSION_CREATED -> {
                this.id = event.getAggregateId();
                this.hostId = (String) event.getPayload().get("hostId");
                this.status = SessionStatus.ACTIVE;
                this.name = (String) event.getPayload().get("name");
                this.description = (String) event.getPayload().get("description");
            }

            case PLAYER_ADDED -> {
                Map<String, Object> payload = event.getPayload();
                Player player = new Player(
                        (String) payload.get("playerId"),
                        (String) payload.get("linkedUserId"),
                        (String) payload.get("displayName"),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        PlayerStatus.ACTIVE,
                        ""
                );
                players.put(player.playerId(), player);
            }

            case BUY_IN_ADDED -> {
                Map<String, Object> payload = event.getPayload();
                String playerId = (String) payload.get("playerId");
                BigDecimal amount = toBigDecimal(payload.get("amount"));

                Player existing = players.get(playerId);
                Player updated = new Player(
                        existing.playerId(),
                        existing.userId(),
                        existing.displayName(),
                        existing.totalBuyIn().add(amount),
                        existing.totalCashOut(),
                        existing.status(),
                        existing.notes()
                );
                players.put(playerId, updated);
            }

            case CASH_OUT_ADDED -> {
                Map<String, Object> payload = event.getPayload();
                String playerId = (String) payload.get("playerId");
                BigDecimal amount = toBigDecimal(payload.get("amount"));

                Player player = players.get(playerId);
                Player updatedPlayer = new Player(
                        player.playerId(),
                        player.userId(),
                        player.displayName(),
                        player.totalBuyIn(),
                        player.totalCashOut().add(amount),
                        player.status(),
                        player.notes()
                );

                players.put(playerId, updatedPlayer);
            }

            case SESSION_CLOSED -> this.status = SessionStatus.CLOSED;

            case SESSION_ARCHIVED -> this.status = SessionStatus.ARCHIVED;

            case SESSION_REOPENED -> this.status = SessionStatus.ACTIVE;

            case PLAYER_REMOVED -> {
                Map<String, Object> payload = event.getPayload();
                String playerId = (String) payload.get("playerId");

                Player player = players.get(playerId);
                Player updatedPlayer = new Player(
                        player.playerId(),
                        player.userId(),
                        player.displayName(),
                        player.totalBuyIn(),
                        player.totalCashOut(),
                        PlayerStatus.INACTIVE,
                        player.notes()
                );

                players.put(playerId, updatedPlayer);
            }

            default ->
                    throw new IllegalStateException("Unknown event type " + event.getEventType());

        }
        this.version++;

    }

    public List<DomainEvent> getUncommittedEvents() {
        return List.copyOf(uncommittedEvents);
    }

    public void markEventsAsCommitted() {
        uncommittedEvents.clear();
        baseVersion = version;
    }
}

// sessionID ==== c8eeeda6-1d9f-446c-a36f-d197e0f86920
// raj === a4a08b52-df66-4c69-90cc-76d39a00f232
// amit === f85f6e0a-66eb-48be-bf13-de30afbd0424
// buyinId === 5c9e90b5-51ae-41fe-851e-99cde3e60952

