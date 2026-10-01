package com.ar.crm2.application.trato.service;

import com.ar.crm2.application.security.CrmAuthorization;

import com.ar.crm2.application.trato.command.DeleteTratoCommand;
import com.ar.crm2.application.trato.exception.TratoNotFoundException;
import com.ar.crm2.application.trato.port.in.DeleteTratoUseCase;
import com.ar.crm2.application.trato.port.out.DeleteTratoByIdPort;
import com.ar.crm2.application.trato.port.out.FindTratoByIdPort;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.vo.TratoId;
import lombok.RequiredArgsConstructor;

/**
 * Application service implementing DeleteTratoUseCase.
 * Validates existence before hard-deleting.
 */
@RequiredArgsConstructor
public class DeleteTratoService implements DeleteTratoUseCase {

    private final CrmAuthorization crmAuthorization;

    private final FindTratoByIdPort findPort;
    private final DeleteTratoByIdPort deletePort;

    @Override
    public void delete(DeleteTratoCommand command) {
        TratoId tratoId = TratoId.from(command.id());
        crmAuthorization.requireRecord(RecursoCrm.TRATO, AccionCrm.ELIMINAR, command.id());

        // Verify trato exists
        findPort.findById(tratoId)
            .orElseThrow(() -> TratoNotFoundException.forId(command.id()));

        deletePort.deleteById(tratoId);
    }
}
