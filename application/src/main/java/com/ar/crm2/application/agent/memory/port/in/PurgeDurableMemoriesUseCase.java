package com.ar.crm2.application.agent.memory.port.in;

import com.ar.crm2.application.agent.memory.command.PurgeDurableMemoriesCommand;

public interface PurgeDurableMemoriesUseCase {
    void purge(PurgeDurableMemoriesCommand command);
}
