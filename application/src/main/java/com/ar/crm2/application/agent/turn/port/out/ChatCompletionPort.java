package com.ar.crm2.application.agent.turn.port.out;

import com.ar.crm2.model.agent.vo.AgentOwnerId;
import com.ar.crm2.model.agent.vo.TurnId;

import java.util.List;

/** Completes a prompt using trusted conversation context. */
@FunctionalInterface
public interface ChatCompletionPort {

    String complete(
            AgentOwnerId ownerId,
            TurnId turnId,
            List<String> visibleHistory,
            List<String> durableMemories,
            String normalizedPrompt
    );
}
