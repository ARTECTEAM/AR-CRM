package com.ar.crm2.application.agent.memory.port.out;

import com.ar.crm2.model.agent.vo.AgentOwnerId;
import com.ar.crm2.model.agent.vo.MemoryId;

public interface DeleteDurableMemoryPort {
    void delete(AgentOwnerId ownerId, MemoryId targetId);
}
