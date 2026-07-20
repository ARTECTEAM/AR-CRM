package com.ar.crm2.model.agent.vo;

import com.ar.crm2.model.agent.entity.AgentTurn;
import com.ar.crm2.shared.DomainAssert;

/** Internal canonical receipt for an accepted user turn. */
public record AcceptedUserTurn(AgentTurn turn, String opaqueHandle) {

    public AcceptedUserTurn {
        DomainAssert.notNull(turn, "agentTurn");
        DomainAssert.notBlank(opaqueHandle, "opaqueHandle");
        opaqueHandle = opaqueHandle.trim();
    }

    @Override
    public String toString() {
        return "AcceptedUserTurn[turn=" + turn + ", opaqueHandle=[REDACTED]]";
    }
}
