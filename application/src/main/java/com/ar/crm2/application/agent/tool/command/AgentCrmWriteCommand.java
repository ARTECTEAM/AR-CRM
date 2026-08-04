package com.ar.crm2.application.agent.tool.command;

import com.ar.crm2.model.agent.vo.AgentOwnerId;
import com.ar.crm2.model.agent.vo.TurnId;

import java.util.UUID;

/** Server-owned command family for agent CRM writes. */
public sealed interface AgentCrmWriteCommand
        permits AgentCrmWriteCommand.UpdateDealStage {

    AgentOwnerId ownerId();

    UUID actorUsuarioId();

    TurnId turnId();

    /** Narrow C1 stage mutation; ownership is checked before delegation. */
    record UpdateDealStage(
            AgentOwnerId ownerId,
            UUID actorUsuarioId,
            TurnId turnId,
            UUID tratoId,
            String status,
            String motivo
    ) implements AgentCrmWriteCommand {
    }
}
