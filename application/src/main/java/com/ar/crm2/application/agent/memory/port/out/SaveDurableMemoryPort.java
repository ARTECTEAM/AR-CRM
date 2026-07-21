package com.ar.crm2.application.agent.memory.port.out;

import com.ar.crm2.model.agent.entity.DurableMemory;

public interface SaveDurableMemoryPort {
    DurableMemory save(DurableMemory memory);
}
