package com.example.backend.handlers;

import com.example.backend.commands.RecordHandCommand;
import com.example.backend.executor.AggregateCommandExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RecordHandHandler {

    private final AggregateCommandExecutor executor;

    public String handle(RecordHandCommand cmd) {
        return executor.execute(cmd.sessionId(), aggregate -> aggregate.handle(cmd));
    }
}