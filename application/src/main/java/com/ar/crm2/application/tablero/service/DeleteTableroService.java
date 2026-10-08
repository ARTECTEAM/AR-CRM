package com.ar.crm2.application.tablero.service;

import com.ar.crm2.application.security.CrmAuthorization;

import com.ar.crm2.application.tablero.command.DeleteTableroCommand;
import com.ar.crm2.application.tablero.exception.TableroNotFoundException;
import com.ar.crm2.application.tablero.port.in.DeleteTableroUseCase;
import com.ar.crm2.application.tablero.port.out.DeleteTableroByIdPort;
import com.ar.crm2.application.tablero.port.out.FindTableroByIdPort;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.vo.TableroId;
import lombok.RequiredArgsConstructor;

/**
 * Application service implementing DeleteTableroUseCase.
 * Validates existence before hard-deleting.
 */
@RequiredArgsConstructor
public class DeleteTableroService implements DeleteTableroUseCase {

    private final CrmAuthorization crmAuthorization;

    private final FindTableroByIdPort findPort;
    private final DeleteTableroByIdPort deletePort;

    @Override
    public void delete(DeleteTableroCommand command) {
        TableroId tableroId = TableroId.from(command.id());
        crmAuthorization.requireRecord(RecursoCrm.TABLERO, AccionCrm.ELIMINAR, command.id());

        // Verify tablero exists
        findPort.findById(tableroId)
            .orElseThrow(() -> TableroNotFoundException.forId(command.id()));

        deletePort.deleteById(tableroId);
    }
}
