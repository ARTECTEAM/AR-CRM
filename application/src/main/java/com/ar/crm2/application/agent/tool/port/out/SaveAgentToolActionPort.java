package com.ar.crm2.application.agent.tool.port.out;

import com.ar.crm2.model.agent.tool.entity.AgentToolAction;

/** Claims a deterministic action and returns the database canonical outcome. */
public interface SaveAgentToolActionPort {

    /**
     * Compatibility name retained from the provisional contract. Implementations
     * must insert once and return the existing canonical row on replay.
     */
    AgentToolAction save(AgentToolAction action);
}
