package com.example.backend.integration;

import com.example.backend.events.DomainEvent;
import com.example.backend.projections.LedgerProjection;
import com.example.backend.repositories.SessionProjectionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LedgerProjectionIdempotencyIntegrationTest extends AbstractMongoIntegrationTest {

    @Autowired
    private LedgerProjection ledgerProjection;

    @Autowired
    private SessionProjectionRepository sessionProjectionRepository;

    @Test
    void redeliveringTheSamePlayerAddedEventTwiceOnlyAppliesItOnce() {
        String sessionId = UUID.randomUUID().toString();
        String playerId = UUID.randomUUID().toString();

        DomainEvent sessionCreated = new DomainEvent(
                UUID.randomUUID().toString(), sessionId, "SESSION", 1, Instant.now(),
                "SessionCreated", "host-1",
                Map.of("name", "Idempotency Test", "hostId", "host-1", "description", "desc"),
                Map.of()
        );

        DomainEvent playerAdded = new DomainEvent(
                UUID.randomUUID().toString(), sessionId, "SESSION", 2, Instant.now(),
                "PlayerAdded", "host-1",
                Map.of("playerId", playerId, "displayName", "Raj", "linkedUserId", ""),
                Map.of()
        );

        ledgerProjection.handle(sessionCreated);
        ledgerProjection.handle(playerAdded);
        ledgerProjection.handle(playerAdded);

        var projection = sessionProjectionRepository.findById(sessionId).orElseThrow();

        assertThat(projection.getPlayers())
                .as("the player should appear exactly once, not duplicated by the redelivered event")
                .hasSize(1);
        assertThat(projection.getLastAppliedVersion()).isEqualTo(2L);
    }

    @Test
    void redeliveringABuyInAddedEventTwiceDoesNotDoubleCountTheAmount() {
        String sessionId = UUID.randomUUID().toString();
        String playerId = UUID.randomUUID().toString();

        ledgerProjection.handle(new DomainEvent(
                UUID.randomUUID().toString(), sessionId, "SESSION", 1, Instant.now(),
                "SessionCreated", "host-1",
                Map.of("name", "Idempotency Test 2", "hostId", "host-1", "description", "desc"),
                Map.of()
        ));
        ledgerProjection.handle(new DomainEvent(
                UUID.randomUUID().toString(), sessionId, "SESSION", 2, Instant.now(),
                "PlayerAdded", "host-1",
                Map.of("playerId", playerId, "displayName", "Raj", "linkedUserId", ""),
                Map.of()
        ));

        DomainEvent buyInAdded = new DomainEvent(
                UUID.randomUUID().toString(), sessionId, "SESSION", 3, Instant.now(),
                "BuyInAdded", "host-1",
                Map.of("playerId", playerId, "amount", new BigDecimal("500"), "buyInId", UUID.randomUUID().toString()),
                Map.of()
        );

        ledgerProjection.handle(buyInAdded);
        ledgerProjection.handle(buyInAdded);

        var projection = sessionProjectionRepository.findById(sessionId).orElseThrow();
        var player = projection.getPlayers().get(0);

        assertThat(player.getTotalBuyIn())
                .as("a redelivered BuyInAdded must not be counted twice — this was a real, silent bug class earlier in this project")
                .isEqualByComparingTo(new BigDecimal("500"));
    }
}
