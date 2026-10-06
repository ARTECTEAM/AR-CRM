package com.ar.crm2.application.agent.memory.port.in;

import com.ar.crm2.application.agent.memory.command.DeleteDurableMemoryCommand;

public interface DeleteDurableMemoryUseCase {
    void delete(DeleteDurableMemoryCommand command);
}
