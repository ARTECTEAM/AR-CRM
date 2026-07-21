package com.ar.crm2.application.agent.memory.service;

import com.ar.crm2.application.agent.memory.command.RecallDurableMemoriesCommand;
import com.ar.crm2.application.agent.memory.port.in.RecallDurableMemoriesUseCase;
import com.ar.crm2.application.agent.memory.port.out.FindEligibleDurableMemoriesPort;
import com.ar.crm2.model.agent.entity.DurableMemory;
import com.ar.crm2.model.agent.vo.AgentOwnerId;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class RecallDurableMemoriesService implements RecallDurableMemoriesUseCase {
    private final FindEligibleDurableMemoriesPort findEligiblePort;

    @Override
    public List<DurableMemory> recall(RecallDurableMemoriesCommand command) {
        return findEligiblePort.findEligible(AgentOwnerId.from(command.actorSubject()));
    }
}
