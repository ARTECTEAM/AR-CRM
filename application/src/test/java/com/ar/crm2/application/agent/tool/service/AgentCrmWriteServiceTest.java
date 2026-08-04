package com.ar.crm2.application.agent.tool.service;

import com.ar.crm2.application.agent.tool.command.AgentCrmWriteCommand;
import com.ar.crm2.application.agent.tool.exception.DealNotOwnedByActorException;
import com.ar.crm2.application.agent.tool.port.in.AgentCrmWriteUseCase;
import com.ar.crm2.application.trato.exception.TratoNotFoundException;
import com.ar.crm2.application.trato.port.in.CambiarEstadoTratoUseCase;
import com.ar.crm2.application.trato.port.out.FindTratoByIdPort;
import com.ar.crm2.model.agent.vo.AgentOwnerId;
import com.ar.crm2.model.agent.vo.TurnId;
import com.ar.crm2.model.entity.Trato;
import com.ar.crm2.model.enums.EstadoTrato;
import com.ar.crm2.model.vo.ContactoId;
import com.ar.crm2.model.vo.TratoId;
import com.ar.crm2.model.vo.UsuarioId;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentCrmWriteServiceTest {

    private static final AgentOwnerId OWNER = AgentOwnerId.from("actor-pr9c4-c1");
    private static final TurnId TURN = TurnId.from(UUID.fromString("00000000-0000-0000-0000-00000000c1c1"));
    private static final UUID TRATO_ID = UUID.fromString("77777777-7777-7777-7777-777777777777");
    private static final UUID ACTOR = UUID.fromString("99999999-9999-9999-9999-999999999999");

    @Test
    void authorizedGanadoReturnsMutatedDeal() {
        FindTratoByIdPort find = mock(FindTratoByIdPort.class);
        CambiarEstadoTratoUseCase mutator = mock(CambiarEstadoTratoUseCase.class);
        Trato existing = deal(ACTOR), result = existing.ganar();
        when(find.findById(TratoId.from(TRATO_ID))).thenReturn(Optional.of(existing));
        when(mutator.ganar(TRATO_ID)).thenReturn(result);

        Trato actual = service(find, mutator).execute(command(ACTOR, "GANADO", null));

        assertSame(result, actual);
        verify(mutator).ganar(TRATO_ID);
        verify(mutator, never()).perder(any(), any());
    }

    @Test
    void authorizedPerdidoForwardsReasonAndReturnsMutatedDeal() {
        FindTratoByIdPort find = mock(FindTratoByIdPort.class);
        CambiarEstadoTratoUseCase mutator = mock(CambiarEstadoTratoUseCase.class);
        Trato existing = deal(ACTOR), result = existing.perder("Budget");
        when(find.findById(TratoId.from(TRATO_ID))).thenReturn(Optional.of(existing));
        when(mutator.perder(TRATO_ID, "Budget")).thenReturn(result);

        assertSame(result, service(find, mutator).execute(command(ACTOR, "PERDIDO", "Budget")));
        verify(mutator).perder(TRATO_ID, "Budget");
        verify(mutator, never()).ganar(any());
    }

    @Test
    void unauthorizedGanadoAndPerdidoFailBeforeMutation() {
        FindTratoByIdPort find = mock(FindTratoByIdPort.class);
        CambiarEstadoTratoUseCase mutator = mock(CambiarEstadoTratoUseCase.class);
        UUID otherOwner = UUID.fromString("00000000-1111-2222-3333-444444444444");
        when(find.findById(TratoId.from(TRATO_ID))).thenReturn(Optional.of(deal(otherOwner)));
        AgentCrmWriteUseCase service = service(find, mutator);

        for (String status : new String[]{"GANADO", "PERDIDO"}) {
            assertThrows(DealNotOwnedByActorException.class,
                    () -> service.execute(command(ACTOR, status, "Budget")));
        }
        verify(mutator, never()).ganar(any());
        verify(mutator, never()).perder(any(), any());
    }

    @Test
    void missingDealFailsBeforeMutation() {
        FindTratoByIdPort find = mock(FindTratoByIdPort.class);
        CambiarEstadoTratoUseCase mutator = mock(CambiarEstadoTratoUseCase.class);
        when(find.findById(TratoId.from(TRATO_ID))).thenReturn(Optional.empty());

        assertThrows(TratoNotFoundException.class,
                () -> service(find, mutator).execute(command(ACTOR, "GANADO", null)));
        verify(mutator, never()).ganar(any());
        verify(mutator, never()).perder(any(), any());
    }

    private static AgentCrmWriteUseCase service(
            FindTratoByIdPort find, CambiarEstadoTratoUseCase mutator) {
        return new AgentCrmWriteService(find, mutator);
    }

    private static AgentCrmWriteCommand.UpdateDealStage command(
            UUID actor, String status, String motivo) {
        return new AgentCrmWriteCommand.UpdateDealStage(
                OWNER, actor, TURN, TRATO_ID, status, motivo);
    }

    private static Trato deal(UUID actor) {
        return Trato.reconstitute(
                TratoId.from(TRATO_ID), ContactoId.create(), UsuarioId.from(actor), "Deal",
                null, null, null, null, EstadoTrato.ABIERTO, null, LocalDateTime.now(), null);
    }
}
