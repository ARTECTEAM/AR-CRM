package com.ar.crm2.application.agent.tool.service;

import com.ar.crm2.application.agent.tool.command.AgentCrmWriteCommand;
import com.ar.crm2.application.agent.tool.exception.DealNotOwnedByActorException;
import com.ar.crm2.application.agent.tool.port.in.AgentCrmWriteUseCase;
import com.ar.crm2.application.trato.exception.TratoNotFoundException;
import com.ar.crm2.application.trato.port.in.CambiarEstadoTratoUseCase;
import com.ar.crm2.application.trato.port.out.FindTratoByIdPort;
import com.ar.crm2.model.entity.Trato;
import com.ar.crm2.model.vo.TratoId;
import lombok.RequiredArgsConstructor;

/**
 * Application boundary for agent deal writes. It loads the deal, requires
 * strict responsable/actor equality, then delegates the existing mutator.
 * C2 ledger/idempotency behavior is intentionally outside this service.
 */
@RequiredArgsConstructor
public final class AgentCrmWriteService implements AgentCrmWriteUseCase {

    private static final String STATUS_GANADO = "GANADO";

    private final FindTratoByIdPort findTratoByIdPort;
    private final CambiarEstadoTratoUseCase cambiarEstadoTratoUseCase;

    @Override
    public Trato execute(AgentCrmWriteCommand command) {
        if (!(command instanceof AgentCrmWriteCommand.UpdateDealStage deal)) {
            throw new IllegalStateException(
                    "Unsupported AgentCrmWriteCommand variant: " + command.getClass().getName());
        }
        Trato trato = findTratoByIdPort.findById(TratoId.from(deal.tratoId()))
                .orElseThrow(() -> TratoNotFoundException.forId(deal.tratoId()));
        if (!trato.getResponsableId().value().equals(deal.actorUsuarioId())) {
            throw new DealNotOwnedByActorException(deal.tratoId(), deal.actorUsuarioId());
        }
        return STATUS_GANADO.equals(deal.status())
                ? cambiarEstadoTratoUseCase.ganar(deal.tratoId())
                : cambiarEstadoTratoUseCase.perder(deal.tratoId(), deal.motivo());
    }
}
