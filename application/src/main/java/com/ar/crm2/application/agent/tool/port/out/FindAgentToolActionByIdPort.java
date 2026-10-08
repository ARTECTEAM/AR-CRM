package com.ar.crm2.application.agent.tool.port.out;

import com.ar.crm2.model.agent.tool.entity.AgentToolAction;
import com.ar.crm2.model.agent.tool.vo.AgentToolActionId;
import com.ar.crm2.model.agent.vo.AgentOwnerId;

import java.util.Optional;

/** Finds an action only within the requested trusted owner scope. */
public interface FindAgentToolActionByIdPort {

    Optional<AgentToolAction> findByOwnerAndId(AgentOwnerId ownerId, AgentToolActionId actionId);
}
