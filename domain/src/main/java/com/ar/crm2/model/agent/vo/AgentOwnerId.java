package com.ar.crm2.model.agent.vo;

import com.ar.crm2.shared.DomainAssert;

/**
 * Immutable backend-derived owner identity for agent conversations.
 */
public record AgentOwnerId(String value) {

    public AgentOwnerId {
        DomainAssert.notBlank(value, "agentOwnerId");
        value = value.trim();
    }

    public static AgentOwnerId from(String value) {
        return new AgentOwnerId(value);
    }
}
