package com.ar.crm2.application.agenda.service;

import com.ar.crm2.application.agenda.command.GetAgendaByIdCommand;
import com.ar.crm2.application.agenda.exception.AgendaNotFoundException;
import com.ar.crm2.application.agenda.port.in.GetAgendaByIdUseCase;
import com.ar.crm2.application.agenda.port.out.FindAgendaByIdPort;
import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Agenda;
import com.ar.crm2.model.vo.AgendaId;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GetAgendaByIdService implements GetAgendaByIdUseCase {

    private final FindAgendaByIdPort findPort;
    private final CrmAuthorization authorization;

    @Override
    public Agenda getById(GetAgendaByIdCommand command) {
        authorization.require(RecursoCrm.AGENDA, AccionCrm.LEER);
        authorization.requireRecord(RecursoCrm.AGENDA, AccionCrm.LEER, command.id());
        AgendaId agendaId = AgendaId.from(command.id());
        Agenda agenda = findPort.findById(agendaId)
                .orElseThrow(() -> AgendaNotFoundException.forId(command.id()));
        if (agenda.getTareaId() != null) {
            authorization.requireRecord(RecursoCrm.TAREA, AccionCrm.LEER, agenda.getTareaId().value());
        }
        if (agenda.getTratoId() != null) {
            authorization.requireRecord(RecursoCrm.TRATO, AccionCrm.LEER, agenda.getTratoId().value());
        }
        return agenda;
    }
}
