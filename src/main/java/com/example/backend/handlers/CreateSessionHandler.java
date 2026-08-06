package com.example.backend.handlers;

import com.example.backend.commands.CreateSessionCommand;
import com.example.backend.executor.AggregateCommandExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CreateSessionHandler {

    private final AggregateCommandExecutor executor;

    public void handle(CreateSessionCommand cmd) {
        executor.execute(cmd.sessionId(), aggregate -> {
            aggregate.handle(cmd);
            return null;
        });
    }

}