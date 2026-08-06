package com.example.backend.controller;

import com.example.backend.commands.*;
import com.example.backend.dto.request.AddBuyInRequest;
import com.example.backend.dto.request.AddCashOutRequest;
import com.example.backend.dto.request.AddPlayerRequest;
import com.example.backend.dto.request.CreateSessionRequest;
import com.example.backend.dto.response.*;
import com.example.backend.handlers.*;
import com.example.backend.helpers.CurrentUserProvider;
import com.example.backend.helpers.IdGenerator;
import com.example.backend.projections.PlayerProjection;
import com.example.backend.projections.SessionProjection;
import com.example.backend.repositories.SessionProjectionRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

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
    private final CloseSessionHandler closeSessionHandler;
    private final ArchiveSessionHandler archiveSessionHandler;
    private final RemovePlayerHandler removePlayerHandler;

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

    @GetMapping("{sessionId}")
    public ResponseEntity<SessionResponse> getSession(@PathVariable String sessionId) {
         SessionProjection projection = sessionProjectionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException(sessionId));

        return ResponseEntity.ok(toResponse(projection));
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
}



