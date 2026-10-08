package com.ar.crm2.application.agent.turn.port.out;

import com.ar.crm2.model.agent.vo.AgentOwnerId;
import com.ar.crm2.model.agent.vo.TurnId;
import com.ar.crm2.model.agent.vo.VisibleMessage;

import java.util.List;

/**
 * Finds the bounded completed visible history while excluding the current
 * turn. Returned entries carry USER/ASSISTANT speaker provenance only —
 * these roles are prompt metadata and never grant or deny authorization.
 */
public interface FindCompletedVisibleHistoryPort {

    List<VisibleMessage> findCompletedVisibleHistory(
            AgentOwnerId ownerId,
            TurnId turnId,
            String opaqueHandle,
            int maximumMessages
    );
}
