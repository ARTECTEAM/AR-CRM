package com.ar.crm2.application.agenda.service;

import com.ar.crm2.application.agenda.command.GetAgendasByUserCommand;
import com.ar.crm2.application.agenda.port.in.GetAgendasByUserUseCase;
import com.ar.crm2.application.agenda.port.out.FindAgendasByUserIdPort;
import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.application.security.CurrentActor;
import com.ar.crm2.application.security.exception.CrmActorUnavailableException;
import com.ar.crm2.application.security.port.out.CurrentActorPort;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Agenda;
import com.ar.crm2.model.vo.UsuarioId;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class GetAgendasByUserService implements GetAgendasByUserUseCase {

    private final FindAgendasByUserIdPort findByUserPort;
    private final CrmAuthorization authorization;
    private final CurrentActorPort currentActorPort;

    @Override
    public List<Agenda> getByUser(GetAgendasByUserCommand command) {
        authorization.require(RecursoCrm.AGENDA, AccionCrm.LEER);
        CurrentActor actor = currentActorPort.currentActor()
                .orElseThrow(() -> new CrmActorUnavailableException("Authenticated CRM user is not active"));
        return findByUserPort.findByCreadoPor(UsuarioId.from(actor.usuarioId())).stream()
                .filter(agenda -> authorization.permitsRecord(
                        RecursoCrm.AGENDA, AccionCrm.LEER, agenda.getId().value()))
                .filter(this::linkedResourcesReadable)
                .toList();
    }

    private boolean linkedResourcesReadable(Agenda agenda) {
        return (agenda.getTareaId() == null || authorization.permitsRecord(
                RecursoCrm.TAREA, AccionCrm.LEER, agenda.getTareaId().value()))
                && (agenda.getTratoId() == null || authorization.permitsRecord(
                RecursoCrm.TRATO, AccionCrm.LEER, agenda.getTratoId().value()));
    }
}
