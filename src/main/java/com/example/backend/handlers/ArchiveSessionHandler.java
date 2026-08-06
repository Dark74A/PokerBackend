package com.example.backend.handlers;

import com.example.backend.commands.ArchiveSessionCommand;
import com.example.backend.executor.AggregateCommandExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ArchiveSessionHandler {

    private final AggregateCommandExecutor executor;

    public void handle(ArchiveSessionCommand command) {
        executor.execute(command.sessionId(), aggregate -> {
            aggregate.handle(command);
            return null;
        });

        return ;
    }
}
