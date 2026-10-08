package com.ar.crm2.application.agent.turn.port.out;

import com.ar.crm2.model.agent.vo.AgentOwnerId;
import com.ar.crm2.model.agent.vo.TurnId;

import java.util.Optional;

/** Finds the canonical assistant content already completed for an owner-bound turn. */
public interface FindCompletedAssistantContentPort {

    Optional<String> findCompletedAssistantContent(AgentOwnerId ownerId, TurnId turnId, String opaqueHandle);
}
