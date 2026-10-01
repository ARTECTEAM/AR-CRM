package com.ar.crm2.adapter.in.rest.mapper;

import com.ar.crm2.adapter.in.rest.dto.request.CreateAgendaRequest;
import com.ar.crm2.adapter.in.rest.dto.request.EditAgendaRequest;
import com.ar.crm2.application.agenda.command.CreateAgendaCommand;
import com.ar.crm2.application.agenda.command.DeleteAgendaCommand;
import com.ar.crm2.application.agenda.command.EditAgendaCommand;
import com.ar.crm2.application.agenda.command.GetAgendaByIdCommand;
import com.ar.crm2.application.security.ActorContext;
import com.ar.crm2.application.security.exception.AuthenticatedUsuarioRequiredException;

import java.util.UUID;

public final class AgendaCommandMapper {

    private AgendaCommandMapper() {}

    public static CreateAgendaCommand toCommand(CreateAgendaRequest request, ActorContext actorContext) {
        if (actorContext == null) {
            throw AuthenticatedUsuarioRequiredException.forMissingActorContext();
        }

        UUID creadoPor = actorContext.usuarioId()
                .orElseThrow(AuthenticatedUsuarioRequiredException::forMissingUsuarioId);
        return new CreateAgendaCommand(
            request.tipo(),
            request.asunto(),
            request.descripcion(),
            request.fecha(),
            request.horaInicio(),
            request.horaFin(),
            request.tareaId(),
            request.tratoId(),
            request.ubicacion(),
            request.linkVideollamada(),
            creadoPor,
            request.recordatorioHabilitado() != null ? request.recordatorioHabilitado() : false,
            request.minutosAntes()
        );
    }

    public static EditAgendaCommand toCommand(UUID id, EditAgendaRequest request) {
        return new EditAgendaCommand(
            id,
            request.tipo(),
            request.asunto(),
            request.descripcion(),
            request.fecha(),
            request.horaInicio(),
            request.horaFin(),
            request.tareaId(),
            request.tratoId(),
            request.ubicacion(),
            request.linkVideollamada(),
            request.recordatorioHabilitado(),
            request.minutosAntes()
        );
    }

    public static DeleteAgendaCommand toDeleteCommand(UUID id) {
        return new DeleteAgendaCommand(id);
    }

    public static GetAgendaByIdCommand toGetByIdCommand(UUID id) {
        return new GetAgendaByIdCommand(id);
    }
}
