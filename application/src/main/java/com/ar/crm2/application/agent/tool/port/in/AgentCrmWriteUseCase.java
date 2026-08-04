package com.ar.crm2.application.agent.tool.port.in;

import com.ar.crm2.application.agent.tool.command.AgentCrmWriteCommand;
import com.ar.crm2.model.entity.Trato;

/** Application boundary for trusted, authorization-gated agent writes. */
public interface AgentCrmWriteUseCase {

    Trato execute(AgentCrmWriteCommand command);
}
