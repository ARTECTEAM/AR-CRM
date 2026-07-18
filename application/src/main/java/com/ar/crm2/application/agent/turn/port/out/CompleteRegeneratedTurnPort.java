package com.ar.crm2.application.agent.turn.port.out;

import com.ar.crm2.model.agent.vo.AgentOwnerId;
import com.ar.crm2.model.agent.vo.TurnId;

/** Atomically converges or updates only the assistant output for an active regeneration. */
public interface CompleteRegeneratedTurnPort {

    String completeRegeneratedTurn(
            AgentOwnerId ownerId,
            TurnId turnId,
            String opaqueHandle,
            String idempotencyKey,
            String assistantContent
    );
}
