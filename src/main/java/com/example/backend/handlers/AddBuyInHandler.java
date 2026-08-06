package com.example.backend.handlers;

import com.example.backend.commands.AddBuyInCommand;
import com.example.backend.executor.AggregateCommandExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AddBuyInHandler {

    private final AggregateCommandExecutor executor;

    public String handle(AddBuyInCommand cmd) {
        return executor.execute(cmd.sessionId(), aggregate -> aggregate.handle(cmd));
    }
}
