package com.ar.crm2.application.agent.memory.service;

import com.ar.crm2.application.agent.memory.command.RememberDurableMemoryCommand;
import com.ar.crm2.application.agent.memory.port.in.RememberDurableMemoryUseCase;
import com.ar.crm2.application.agent.memory.port.out.SaveDurableMemoryPort;
import com.ar.crm2.model.agent.entity.DurableMemory;
import com.ar.crm2.model.agent.vo.AgentOwnerId;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RememberDurableMemoryService implements RememberDurableMemoryUseCase {
    private final SaveDurableMemoryPort savePort;

    @Override
    public DurableMemory remember(RememberDurableMemoryCommand command) {
        return savePort.save(DurableMemory.create(
                AgentOwnerId.from(command.actorSubject()), command.content(), command.safetyContext()));
    }
}
