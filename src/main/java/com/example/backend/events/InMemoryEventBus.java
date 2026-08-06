package com.example.backend.events;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class InMemoryEventBus implements EventBus {

    private final Map<String, List<EventHandler>> handlers = new HashMap<>();

    @Override
    public void subscribe(EventHandler handler) {
        for (String eventType : handler.supportedEventTypes()) {
            handlers
                    .computeIfAbsent(eventType, k -> new ArrayList<>())
                    .add(handler);
        }
    }

    @Override
    public void publish(List<DomainEvent> events) {
        for (DomainEvent event : events) {
            List<EventHandler> subscribers =
                    handlers.getOrDefault(event.getEventType(), List.of());

            for (EventHandler handler : subscribers) {
                try {
                    handler.handle(event);
                } catch (Exception ex) {
                    log.error(
                            "Projection {} failed while handling {}",
                            handler.getClass().getSimpleName(),
                            event.getEventType(),
                            ex
                    );
                }
            }
        }
    }
}