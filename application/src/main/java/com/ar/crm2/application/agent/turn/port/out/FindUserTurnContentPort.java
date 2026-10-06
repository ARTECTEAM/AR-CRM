package com.ar.crm2.application.agent.turn.port.out;

import com.ar.crm2.model.agent.vo.AgentOwnerId;
import com.ar.crm2.model.agent.vo.TurnId;

/** Finds the persisted original user content for one owner-bound turn. */
public interface FindUserTurnContentPort {

    String findUserTurnContent(AgentOwnerId ownerId, TurnId turnId, String opaqueHandle);
}
