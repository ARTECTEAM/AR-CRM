package com.ar.crm2.application.agent.turn.port.out;

import com.ar.crm2.model.agent.entity.AgentTurn;
import com.ar.crm2.model.agent.entity.Conversation;
import com.ar.crm2.model.agent.vo.AgentOwnerId;

/** Atomically converges a submitted user turn or persists its domain candidates. */
public interface CreateUserTurnPort {

    AgentTurn createOrGetUserTurn(
            Conversation conversation,
            AgentTurn turn,
            AgentOwnerId ownerId,
            String idempotencyKey,
            String payloadFingerprint,
            String opaqueHandle
    );
}
