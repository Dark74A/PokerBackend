package com.example.backend.events;

import com.example.backend.events.EventBus;
import com.example.backend.events.EventHandler;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class EventHandlerRegistrar {

    private final EventBus eventBus;
    private final List<EventHandler> handlers;

    @PostConstruct
    public void registerAll() {
        handlers.forEach(eventBus::subscribe);
    }
}