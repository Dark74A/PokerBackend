package com.example.backend.projections;

import com.example.backend.events.DomainEvent;
import com.example.backend.events.EventBus;
import com.example.backend.events.EventHandler;
import com.example.backend.events.EventStore;
import com.example.backend.repositories.HistoryEntryRepository;
import com.example.backend.repositories.SessionProjectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectionRebuilder {

    private final EventStore eventStore;
    private final SessionProjectionRepository sessionProjectionRepository;
    private final HistoryEntryRepository historyEntryRepository;
    private final EventBus eventBus;

    public void rebuildForSession(String sessionId) {
        log.info("Rebuilding projections for session {}", sessionId);
        
        sessionProjectionRepository.deleteById(sessionId);
        historyEntryRepository.deleteBySessionId(sessionId);
        
        List<DomainEvent> events = eventStore.loadEvents(sessionId);
        eventBus.publish(events);
        log.info("Rebuilt projections for session {} from {} events", 
                 sessionId, events.size());
    }

}