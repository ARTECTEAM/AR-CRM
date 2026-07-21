package com.ar.crm2.application.agent.memory.port.in;

import com.ar.crm2.application.agent.memory.command.RecallDurableMemoriesCommand;
import com.ar.crm2.model.agent.entity.DurableMemory;

import java.util.List;

public interface RecallDurableMemoriesUseCase {
    List<DurableMemory> recall(RecallDurableMemoriesCommand command);
}
