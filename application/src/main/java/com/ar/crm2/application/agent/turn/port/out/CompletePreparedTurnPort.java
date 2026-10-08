package com.ar.crm2.application.agent.turn.port.out;

import com.ar.crm2.model.agent.vo.AgentOwnerId;
import com.ar.crm2.model.agent.vo.TurnId;

/** Atomically completes a prepared turn or returns its canonical assistant content after convergence. */
public interface CompletePreparedTurnPort {

    String completePreparedTurn(AgentOwnerId ownerId, TurnId turnId, String opaqueHandle, String assistantContent);
}
