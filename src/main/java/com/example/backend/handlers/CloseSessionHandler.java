package com.example.backend.handlers;

import com.example.backend.commands.CloseSessionCommand;
import com.example.backend.executor.AggregateCommandExecutor;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class CloseSessionHandler {

    private final AggregateCommandExecutor executor;

    public void handle(CloseSessionCommand command) {
        executor.execute(command.sessionId(), aggregate -> {
            aggregate.handle(command);
            return null;
        });
        return ;
    }
}
