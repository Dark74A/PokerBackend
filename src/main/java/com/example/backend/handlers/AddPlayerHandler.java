package com.example.backend.handlers;

import com.example.backend.commands.AddPlayerCommand;
import com.example.backend.executor.AggregateCommandExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AddPlayerHandler {

    private final AggregateCommandExecutor executor;

    public String handle(AddPlayerCommand cmd) {
        return executor.execute(cmd.sessionId(), aggregate -> aggregate.handle(cmd));
    }

}
