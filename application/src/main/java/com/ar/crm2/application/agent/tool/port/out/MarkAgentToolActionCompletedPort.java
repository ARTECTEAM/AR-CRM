package com.ar.crm2.application.agent.tool.port.out;

import com.ar.crm2.model.agent.tool.entity.AgentToolAction;
import com.ar.crm2.model.agent.tool.vo.AgentToolActionId;
import com.ar.crm2.model.agent.tool.vo.AgentToolResource;
import com.ar.crm2.model.agent.vo.AgentOwnerId;

/** Completes an owner-scoped action and returns its immutable canonical outcome. */
public interface MarkAgentToolActionCompletedPort {

    AgentToolAction markCompleted(AgentOwnerId ownerId, AgentToolActionId actionId,
                                  AgentToolResource resource);
}
