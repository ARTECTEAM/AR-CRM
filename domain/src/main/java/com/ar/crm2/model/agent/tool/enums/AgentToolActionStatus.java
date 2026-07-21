package com.ar.crm2.model.agent.tool.enums;

/**
 * Lifecycle of an {@code AgentToolAction}. The action enters {@code PENDING}
 * when the server first claims a deterministic identity for an owner/turn/
 * tool/canonical-argument quadruple, and transitions to {@code COMPLETED}
 * after the canonical resource has been recorded. The lifecycle is closed
 * and immutable: once the action is {@code COMPLETED} the canonical
 * resource and timestamp are fixed for the lifetime of the row.
 */
public enum AgentToolActionStatus {

    PENDING,
    COMPLETED;

    public boolean isTerminal() {
        return this == COMPLETED;
    }
}
