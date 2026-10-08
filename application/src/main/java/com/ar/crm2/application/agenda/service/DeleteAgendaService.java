package com.ar.crm2.application.agenda.service;

import com.ar.crm2.application.agenda.command.DeleteAgendaCommand;
import com.ar.crm2.application.agenda.exception.AgendaNotFoundException;
import com.ar.crm2.application.agenda.port.in.DeleteAgendaUseCase;
import com.ar.crm2.application.agenda.port.out.DeleteAgendaByIdPort;
import com.ar.crm2.application.agenda.port.out.FindAgendaByIdPort;
import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.vo.AgendaId;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DeleteAgendaService implements DeleteAgendaUseCase {

    private final FindAgendaByIdPort findPort;
    private final DeleteAgendaByIdPort deletePort;
    private final CrmAuthorization authorization;

    @Override
    public void delete(DeleteAgendaCommand command) {
        authorization.require(RecursoCrm.AGENDA, AccionCrm.ELIMINAR);
        authorization.requireRecord(RecursoCrm.AGENDA, AccionCrm.ELIMINAR, command.id());
        AgendaId agendaId = AgendaId.from(command.id());

        findPort.findById(agendaId)
                .orElseThrow(() -> AgendaNotFoundException.forId(command.id()));

        deletePort.deleteById(agendaId);
    }
}
