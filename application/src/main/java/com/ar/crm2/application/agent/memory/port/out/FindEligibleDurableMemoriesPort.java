package com.ar.crm2.application.agent.memory.port.out;

import com.ar.crm2.model.agent.entity.DurableMemory;
import com.ar.crm2.model.agent.vo.AgentOwnerId;

import java.util.List;

public interface FindEligibleDurableMemoriesPort {
    List<DurableMemory> findEligible(AgentOwnerId ownerId);
}
