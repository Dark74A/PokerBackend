package com.example.backend.integration;

import com.example.backend.commands.AddPlayerCommand;
import com.example.backend.commands.CreateSessionCommand;
import com.example.backend.events.DomainEvent;
import com.example.backend.events.EventStore;
import com.example.backend.executor.AggregateCommandExecutor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = "debug=true")
class EventStoreConcurrencyIntegrationTest extends AbstractMongoIntegrationTest {

    @Autowired
    private AggregateCommandExecutor executor;

    @Autowired
    private EventStore eventStore;

    @Test
    @DisplayName("at realistic contention (5 writers), all succeed via retry with no lost or duplicated versions")
    void moderateContentionAllSucceed() throws InterruptedException {
        runConcurrentWriters(5, /* expectAllToSucceed */ true);
    }

    private void runConcurrentWriters(int concurrentWriters, boolean expectAllToSucceed) throws InterruptedException {
        String sessionId = UUID.randomUUID().toString();
        String hostId = "concurrency-test-host";

        executor.execute(sessionId, aggregate -> {
            aggregate.handle(new CreateSessionCommand(sessionId, hostId, "Concurrency Test", "desc"));
            return null;
        });

        ExecutorService threadPool = Executors.newFixedThreadPool(concurrentWriters);
        CountDownLatch readyLatch = new CountDownLatch(concurrentWriters);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failureCount = new AtomicInteger();

        IntStream.range(0, concurrentWriters).forEach(i -> threadPool.submit(() -> {
            try {
                readyLatch.countDown();
                startLatch.await();
                executor.execute(sessionId, aggregate -> {
                    aggregate.handle(new AddPlayerCommand(sessionId, hostId, null, "Player-" + i));
                    return null;
                });
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            }
        }));

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();
        threadPool.shutdown();
        assertThat(threadPool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        if (expectAllToSucceed) {
            assertThat(failureCount.get())
                    .as("at this contention level, the retry budget should absorb every conflict")
                    .isZero();
        }

        List<DomainEvent> allEvents = eventStore.loadEvents(sessionId);
        assertThat(allEvents).hasSize(1 + successCount.get()); // SessionCreated + however many actually succeeded

        List<Long> versions = allEvents.stream().map(DomainEvent::getVersion).sorted().toList();
        assertThat(versions).doesNotHaveDuplicates();
        assertThat(versions).isEqualTo(
                java.util.stream.LongStream.rangeClosed(1, allEvents.size()).boxed().toList()
        );
    }


    @Test
    @DisplayName("at extreme contention (8 writers), some may exhaust retries, but whatever persists is never corrupted")
    void extremeContentionNeverCorruptsData() throws InterruptedException {
        runConcurrentWriters(8, false);
    }

}
