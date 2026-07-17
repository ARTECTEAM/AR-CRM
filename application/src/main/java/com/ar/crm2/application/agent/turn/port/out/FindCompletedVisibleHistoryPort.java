package com.ar.crm2.application.agent.turn.port.out;

import com.ar.crm2.model.agent.vo.AgentOwnerId;
import com.ar.crm2.model.agent.vo.TurnId;

import java.util.List;

/** Finds bounded completed visible history while excluding the current turn. */
public interface FindCompletedVisibleHistoryPort {

    List<String> findCompletedVisibleHistory(
            AgentOwnerId ownerId,
            TurnId turnId,
            String opaqueHandle,
            int maximumMessages
    );
}
