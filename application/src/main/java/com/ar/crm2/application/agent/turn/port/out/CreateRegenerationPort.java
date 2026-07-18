package com.ar.crm2.application.agent.turn.port.out;

import com.ar.crm2.model.agent.vo.AgentOwnerId;
import com.ar.crm2.model.agent.vo.TurnId;

import java.util.Optional;

/** Atomically starts the next sequential regeneration or returns the same-key canonical content. */
public interface CreateRegenerationPort {

    Optional<String> createRegenerationOrFindCanonical(
            AgentOwnerId ownerId,
            TurnId turnId,
            String opaqueHandle,
            String idempotencyKey
    );
}
