package com.ar.crm2.application.agent.memory.port.in;

import com.ar.crm2.application.agent.memory.command.RememberDurableMemoryCommand;
import com.ar.crm2.model.agent.entity.DurableMemory;

public interface RememberDurableMemoryUseCase {
    DurableMemory remember(RememberDurableMemoryCommand command);
}
