package com.ar.crm2.application.agent.memory.service;

import com.ar.crm2.application.agent.memory.command.DeleteDurableMemoryCommand;
import com.ar.crm2.application.agent.memory.port.in.DeleteDurableMemoryUseCase;
import com.ar.crm2.application.agent.memory.port.out.DeleteDurableMemoryPort;
import com.ar.crm2.model.agent.vo.AgentOwnerId;
import com.ar.crm2.model.agent.vo.MemoryId;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DeleteDurableMemoryService implements DeleteDurableMemoryUseCase {
    private final DeleteDurableMemoryPort deletePort;

    @Override
    public void delete(DeleteDurableMemoryCommand command) {
        deletePort.delete(AgentOwnerId.from(command.actorSubject()), MemoryId.from(command.memoryId()));
    }
}
