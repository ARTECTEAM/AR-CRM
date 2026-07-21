package com.ar.crm2.application.agent.turn.port.out;

import com.ar.crm2.model.agent.vo.AgentOwnerId;
import com.ar.crm2.model.agent.vo.TurnId;
import com.ar.crm2.model.agent.vo.VisibleMessage;

import java.util.List;

/**
 * Completes a prompt using trusted conversation context. The visible history
 * arrives already ordered oldest-first and explicitly tagged with the speaker
 * provenance; the adapter must not interpret the role field as authorization.
 */
@FunctionalInterface
public interface ChatCompletionPort {

    String complete(
            AgentOwnerId ownerId,
            TurnId turnId,
            List<VisibleMessage> visibleHistory,
            List<String> durableMemories,
            String normalizedPrompt
    );
}
