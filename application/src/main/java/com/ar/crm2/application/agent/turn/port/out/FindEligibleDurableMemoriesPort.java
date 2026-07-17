package com.ar.crm2.application.agent.turn.port.out;

import com.ar.crm2.model.agent.vo.AgentOwnerId;

import java.util.List;

/** Finds all eligible durable-memory context values in stable order for one owner. */
public interface FindEligibleDurableMemoriesPort {

    List<String> findEligibleDurableMemories(AgentOwnerId ownerId);
}
