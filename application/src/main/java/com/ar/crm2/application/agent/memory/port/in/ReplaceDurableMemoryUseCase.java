package com.ar.crm2.application.agent.memory.port.in;

import com.ar.crm2.application.agent.memory.command.ReplaceDurableMemoryCommand;
import com.ar.crm2.model.agent.entity.DurableMemory;

public interface ReplaceDurableMemoryUseCase {
    DurableMemory replace(ReplaceDurableMemoryCommand command);
}
