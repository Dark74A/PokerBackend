package com.example.backend.executor;

import com.example.backend.aggregate.SessionAggregate;
import com.example.backend.repositories.SessionRepository;
import com.example.backend.events.DomainEvent;
import com.example.backend.events.EventBus;
import com.example.backend.exception.ConcurrencyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class AggregateCommandExecutor {

    private final SessionRepository repository;
    private final EventBus eventBus;

    private static final int MAX_RETRIES = 3;
    private static final long BASE_DELAY_MS = 20;

    private void sleepWithJitter(int attempt) {
        long delay = BASE_DELAY_MS * attempt;

        long jitter = ThreadLocalRandom.current().nextLong(0, BASE_DELAY_MS);

        try {
            Thread.sleep(delay + jitter);
        } catch (InterruptedException err) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Retry Interrupted", err);
        }
    }

    public <T> T execute(String sessionId, Function<SessionAggregate, T> action) {

        int attempts = 0;

        while (true) {

            try {
                SessionAggregate aggregate = repository.load(sessionId);
                T result = action.apply(aggregate);

                List<DomainEvent> persistedEvents = repository.save(aggregate);
                eventBus.publish(persistedEvents);
                aggregate.markEventsAsCommitted();

                return result;

            } catch (ConcurrencyException ex) {


                if (++attempts > MAX_RETRIES) {
                    throw ex;
                }

                sleepWithJitter(attempts);
            }
        }
    }
}