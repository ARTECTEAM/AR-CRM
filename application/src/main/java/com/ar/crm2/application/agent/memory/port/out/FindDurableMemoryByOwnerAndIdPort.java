package com.ar.crm2.application.agent.memory.port.out;

import com.ar.crm2.model.agent.entity.DurableMemory;
import com.ar.crm2.model.agent.vo.AgentOwnerId;
import com.ar.crm2.model.agent.vo.MemoryId;

import java.util.Optional;

public interface FindDurableMemoryByOwnerAndIdPort {
    Optional<DurableMemory> findByOwnerAndId(AgentOwnerId ownerId, MemoryId memoryId);
}
