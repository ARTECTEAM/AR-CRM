package com.ar.crm2.application.tarea.service;

import com.ar.crm2.application.security.CrmAuthorization;

import com.ar.crm2.application.tarea.command.GetTareaByIdCommand;
import com.ar.crm2.application.tarea.exception.TareaNotFoundException;
import com.ar.crm2.application.tarea.port.in.GetTareaByIdUseCase;
import com.ar.crm2.application.tarea.port.out.FindTareaByIdPort;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Tarea;
import com.ar.crm2.model.vo.TareaId;
import lombok.RequiredArgsConstructor;

/**
 * Application service implementing GetTareaByIdUseCase.
 * Loads a Tarea by id or throws TareaNotFoundException.
 */
@RequiredArgsConstructor
public class GetTareaByIdService implements GetTareaByIdUseCase {

    private final CrmAuthorization crmAuthorization;

    private final FindTareaByIdPort findPort;

    @Override
    public Tarea getById(GetTareaByIdCommand command) {
        TareaId tareaId = TareaId.from(command.id());
        crmAuthorization.requireRecord(RecursoCrm.TAREA, AccionCrm.LEER, command.id());

        var tarea = findPort.findById(tareaId)
            .orElseThrow(() -> TareaNotFoundException.forId(command.id()));
        crmAuthorization.requireRecord(RecursoCrm.TRATO, AccionCrm.LEER, tarea.getTratoId().value());
        return tarea;
    }
}
