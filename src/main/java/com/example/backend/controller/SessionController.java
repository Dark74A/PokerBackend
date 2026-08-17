package com.example.backend.controller;

import com.example.backend.aggregate.SessionAggregate;
import com.example.backend.commands.*;
import com.example.backend.dto.request.*;
import com.example.backend.dto.response.*;
import com.example.backend.events.HistoryEntry;
import com.example.backend.exception.SessionNotFoundException;
import com.example.backend.exception.UnauthorizedActionException;
import com.example.backend.handlers.*;
import com.example.backend.helpers.CurrentUserProvider;
import com.example.backend.helpers.IdGenerator;
import com.example.backend.projections.PlayerProjection;
import com.example.backend.projections.ProjectionRebuilder;
import com.example.backend.projections.SessionProjection;
import com.example.backend.repositories.HistoryEntryRepository;
import com.example.backend.repositories.SessionProjectionRepository;
import com.example.backend.repositories.SessionRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final CreateSessionHandler createSessionHandler;
    private final AddPlayerHandler addPlayerHandler;
    private final IdGenerator idGenerator;
    private final CurrentUserProvider currentUserProvider;
    private final AddBuyInHandler addBuyInHandler;
    private final AddCashOutHandler addCashOutHandler;
    private final ReopenSessionHandler reopenSessionHandler;

    private final SessionProjectionRepository sessionProjectionRepository;
    private final SessionRepository sessionRepository;

    private final CloseSessionHandler closeSessionHandler;
    private final ArchiveSessionHandler archiveSessionHandler;
    private final RemovePlayerHandler removePlayerHandler;
    private final RecordHandHandler recordHandHandler;
    private final HistoryEntryRepository historyEntryRepository;

    private final ProjectionRebuilder projectionRebuilder;

    @PostMapping
    public ResponseEntity<CreateSessionResponse> createSession(@Valid @RequestBody CreateSessionRequest request) {

        String sessionId = idGenerator.nextId();
        String hostId = currentUserProvider.getCurrentUserId();

        CreateSessionCommand command = new CreateSessionCommand(
                sessionId,
                hostId,
                request.name(),
                request.description()
        );

        createSessionHandler.handle(command);

        return ResponseEntity
                .created(URI.create("/api/sessions/" + sessionId))
                .body(new CreateSessionResponse(sessionId));
    }

    @PostMapping("{sessionId}/players")
    public ResponseEntity<AddPlayerResponse> addPlayerToSession(@PathVariable String sessionId, @Valid @RequestBody AddPlayerRequest request) {

        String hostId = currentUserProvider.getCurrentUserId();

        AddPlayerCommand command = new AddPlayerCommand(
                sessionId,
                hostId,
                request.linkedUserId(),
                request.displayName()
        );

        String playerId = addPlayerHandler.handle(command);
        return ResponseEntity.created(
                URI.create("/api/sessions/" + sessionId + "/players/" + playerId)
        ).body(new AddPlayerResponse(playerId));
    }

    @PostMapping("{sessionId}/buyins")
    public ResponseEntity<AddBuyInResponse> addBuyIns(@PathVariable String sessionId, @Valid @RequestBody AddBuyInRequest request) {

        String hostId = currentUserProvider.getCurrentUserId();

        AddBuyInCommand command = new AddBuyInCommand(
                sessionId,
                hostId,
                request.playerId(),
                request.amount()
        );

        String buyInId = addBuyInHandler.handle(command);

        return ResponseEntity
                .created(URI.create("/api/sessions/" + sessionId + "/buyins/" + buyInId))
                .body(new AddBuyInResponse(buyInId));
    }

    @PostMapping("{sessionId}/cashouts")
    public ResponseEntity<AddCashOutResponse> addCashOut(@PathVariable String sessionId, @Valid @RequestBody AddCashOutRequest request) {
        String hostId = currentUserProvider.getCurrentUserId();

        AddCashOutCommand command = new AddCashOutCommand(
                sessionId,
                hostId,
                request.playerId(),
                request.amount()
        );

        String cashOutId = addCashOutHandler.handle(command);

        return ResponseEntity
                .created(URI.create("/api/sessions/" + sessionId + "/cashouts/" + cashOutId))
                .body(new AddCashOutResponse(cashOutId));
    }

    private SessionResponse toResponse(SessionProjection projection) {

        List<PlayerProjection> playerProjection = projection.getPlayers();

        List<PlayerResponse> playerResponses = playerProjection.stream().map(p -> new PlayerResponse(
                p.getPlayerId(),
                p.getDisplayName(),
                p.getTotalBuyIn(),
                p.getTotalCashOut(),
                p.getTotalCashOut().subtract(p.getTotalBuyIn()),
                p.getChipStack(),
                p.getStatus()
        )).toList();


        BigDecimal totalBuyIns = playerResponses.stream()
                .map(PlayerResponse::totalBuyIn)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCashOuts = playerResponses.stream()
                .map(PlayerResponse::totalCashOut)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new SessionResponse(
                projection.getSessionId(),
                projection.getSessionName(),
                projection.getHostId(),
                projection.getStatus(),
                playerResponses,
                totalBuyIns,
                totalCashOuts
        );

    }

    @PostMapping("{sessionId}/reopen")
    public ResponseEntity<ReopenSessionResponse> reopenSession(@PathVariable String sessionId) {
        String hostId = currentUserProvider.getCurrentUserId();
        ReopenSessionCommand command = new ReopenSessionCommand(sessionId, hostId);
        reopenSessionHandler.handle(command);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("{sessionId}/close")
    public ResponseEntity<CloseSessionResponse> closeSession(@PathVariable String sessionId) {

        String hostId = currentUserProvider.getCurrentUserId();

        CloseSessionCommand command = new CloseSessionCommand(sessionId, hostId);
        closeSessionHandler.handle(command);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("{sessionId}/archive")
    public ResponseEntity<ArchiveSessionResponse> archiveSession(@PathVariable String sessionId) {

        String hostId = currentUserProvider.getCurrentUserId();

        ArchiveSessionCommand command = new ArchiveSessionCommand(sessionId, hostId);

        archiveSessionHandler.handle(command);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<SessionSummaryResponse>> getSessions() {

        String currentUserId = currentUserProvider.getCurrentUserId();

        List<SessionProjection> sessionProjections = sessionProjectionRepository.findByHostIdOrParticipant(currentUserId);

        List<SessionSummaryResponse> responses = sessionProjections.stream().map(s -> new SessionSummaryResponse(
                    s.getSessionId(),
                    s.getSessionName(),
                    s.getStatus(),
                    s.getCreatedAt()
            )
        ).toList();

        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("{sessionId}/players/{playerId}")
    public ResponseEntity<Void> removePlayer(@PathVariable String sessionId, @PathVariable String playerId) {

        String hostId = currentUserProvider.getCurrentUserId();

        RemovePlayerCommand cmd = new RemovePlayerCommand(sessionId, hostId, playerId);
        removePlayerHandler.handle(cmd);

        return ResponseEntity.noContent().build();
    }


    private SessionResponse toHistoricalResponse(SessionAggregate aggregate) {

        List<PlayerResponse> playerResponses = aggregate.getPlayers().values().stream()
                .map(p -> new PlayerResponse(
                        p.playerId(),
                        p.displayName(),
                        p.totalBuyIn(),
                        p.totalCashOut(),
                        p.totalCashOut().subtract(p.totalBuyIn()),
                        p.chipStack(),
                        p.status().name()
                ))
                .toList();

        BigDecimal totalBuyIns = playerResponses.stream()
                .map(PlayerResponse::totalBuyIn)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCashOuts = playerResponses.stream()
                .map(PlayerResponse::totalCashOut)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new SessionResponse(
                aggregate.getId(),
                aggregate.getName(),
                aggregate.getHostId(),
                aggregate.getStatus().name(),
                playerResponses,
                totalBuyIns,
                totalCashOuts
        );
    }

    @GetMapping("{sessionId}")
    public ResponseEntity<SessionResponse> getSession(@PathVariable String sessionId, @RequestParam(required = false) Instant asOf) {
        if (asOf != null) {
            SessionAggregate aggregate = sessionRepository.loadAsOf(sessionId, asOf);
            return ResponseEntity.ok(toHistoricalResponse(aggregate));
        }

        SessionProjection projection = sessionProjectionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));
        return ResponseEntity.ok(toResponse(projection));
    }

    @PostMapping("{sessionId}/hands")
    public ResponseEntity<RecordHandResponse> recordHand(@PathVariable String sessionId, @Valid @RequestBody RecordHandRequest request) {
        String hostId = currentUserProvider.getCurrentUserId();

        RecordHandCommand command = new RecordHandCommand(
                sessionId,
                hostId,
                request.deltas()
        );

        String handId = recordHandHandler.handle(command);

        return ResponseEntity
                .created(URI.create("/api/sessions/" + sessionId + "/hands/" + handId))
                .body(new RecordHandResponse(handId));
    }

    @GetMapping("{sessionId}/history")
    public ResponseEntity<List<HistoryEntryResponse>> getHistory(@PathVariable String sessionId) {
        List<HistoryEntry> entries = historyEntryRepository.findBySessionIdOrderByVersionAsc(sessionId);

        List<HistoryEntryResponse> response = entries.stream()
                .map(e -> new HistoryEntryResponse(e.getEventType(), e.getDescription(), e.getTimestamp()))
                .toList();

        return ResponseEntity.ok(response);
    }

    @PostMapping("{sessionId}/rebuild-projections")
    public ResponseEntity<Void> rebuildProjections(@PathVariable String sessionId) {
        String currentUserId = currentUserProvider.getCurrentUserId();

        SessionAggregate aggregate = sessionRepository.load(sessionId);
        if (aggregate.getId() == null) {
            throw new SessionNotFoundException(sessionId);
        }
        if (!aggregate.getHostId().equals(currentUserId)) {
            throw new UnauthorizedActionException("Only the host can rebuild this session's projections.");
        }

        projectionRebuilder.rebuildForSession(sessionId);
        return ResponseEntity.noContent().build();
    }
}



