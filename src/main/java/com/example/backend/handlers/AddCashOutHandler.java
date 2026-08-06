package com.example.backend.handlers;


import com.example.backend.commands.AddCashOutCommand;
import com.example.backend.executor.AggregateCommandExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AddCashOutHandler {

    private final AggregateCommandExecutor executor;

    public String handle(AddCashOutCommand cmd) {
        return executor.execute(cmd.sessionId(), aggregate -> aggregate.handle(cmd));
    }
}

