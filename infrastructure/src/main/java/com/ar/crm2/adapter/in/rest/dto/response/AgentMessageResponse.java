package com.ar.crm2.adapter.in.rest.dto.response;

/**
 * REST response DTO for the Pipely CRM conversational ingress. Carries
 * ONLY the final assistant content — internal handles, turn identifiers,
 * owner subject, actor identity, durable memory snapshots, visible
 * history, idempotency keys, and tool-action ledger rows remain private
 * to the Application layer per the agent-conversation "Internal-Only
 * Stages" requirement.
 */
public record AgentMessageResponse(String content) {

    public static AgentMessageResponse of(String content) {
        return new AgentMessageResponse(content);
    }
}