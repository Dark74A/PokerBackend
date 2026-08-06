package com.example.backend.handlers;

import com.example.backend.commands.RemovePlayerCommand;
import com.example.backend.executor.AggregateCommandExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RemovePlayerHandler {

    private final AggregateCommandExecutor executor;

    public void handle(RemovePlayerCommand command) {
        executor.execute(command.sessionId(), aggregate -> {
            aggregate.handle(command);
            return null;
        });
        return;
    }
}
