package com.example.backend.handlers;

import com.example.backend.commands.ReopenSessionCommand;
import com.example.backend.executor.AggregateCommandExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReopenSessionHandler {

    private final AggregateCommandExecutor executor;

    public void handle(ReopenSessionCommand cmd) {
        executor.execute(cmd.sessionId(), aggregate -> {
            aggregate.handle(cmd);
            return null;
        }
    );
        return;
    }
}

