package com.ar.crm2.application.agent.memory.service;

import com.ar.crm2.application.agent.memory.command.ReplaceDurableMemoryCommand;
import com.ar.crm2.application.agent.memory.port.in.ReplaceDurableMemoryUseCase;
import com.ar.crm2.application.agent.memory.port.out.ReplaceDurableMemoryPort;
import com.ar.crm2.model.agent.entity.DurableMemory;
import com.ar.crm2.model.agent.vo.AgentOwnerId;
import com.ar.crm2.model.agent.vo.MemoryId;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ReplaceDurableMemoryService implements ReplaceDurableMemoryUseCase {
    private final ReplaceDurableMemoryPort replacePort;

    @Override
    public DurableMemory replace(ReplaceDurableMemoryCommand command) {
        AgentOwnerId ownerId = AgentOwnerId.from(command.actorSubject());
        DurableMemory replacement = DurableMemory.create(ownerId, command.content(), command.safetyContext());
        return replacePort.replace(ownerId, MemoryId.from(command.memoryId()), replacement);
    }
}
