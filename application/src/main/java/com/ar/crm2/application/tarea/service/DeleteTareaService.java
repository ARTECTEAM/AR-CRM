package com.ar.crm2.application.tarea.service;

import com.ar.crm2.application.security.CrmAuthorization;

import com.ar.crm2.application.tarea.command.DeleteTareaCommand;
import com.ar.crm2.application.tarea.exception.TareaNotFoundException;
import com.ar.crm2.application.tarea.port.in.DeleteTareaUseCase;
import com.ar.crm2.application.tarea.port.out.DeleteTareaByIdPort;
import com.ar.crm2.application.tarea.port.out.FindTareaByIdPort;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.vo.TareaId;
import lombok.RequiredArgsConstructor;

/**
 * Application service implementing DeleteTareaUseCase.
 * Validates existence before hard-deleting.
 */
@RequiredArgsConstructor
public class DeleteTareaService implements DeleteTareaUseCase {

    private final CrmAuthorization crmAuthorization;

    private final FindTareaByIdPort findPort;
    private final DeleteTareaByIdPort deletePort;

    @Override
    public void delete(DeleteTareaCommand command) {
        TareaId tareaId = TareaId.from(command.id());
        crmAuthorization.requireRecord(RecursoCrm.TAREA, AccionCrm.ELIMINAR, command.id());

        // Verify tarea exists
        findPort.findById(tareaId)
            .orElseThrow(() -> TareaNotFoundException.forId(command.id()));

        deletePort.deleteById(tareaId);
    }
}
