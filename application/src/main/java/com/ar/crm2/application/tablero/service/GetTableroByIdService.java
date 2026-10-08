package com.ar.crm2.application.tablero.service;

import com.ar.crm2.application.security.CrmAuthorization;

import com.ar.crm2.application.tablero.command.GetTableroByIdCommand;
import com.ar.crm2.application.tablero.exception.TableroNotFoundException;
import com.ar.crm2.application.tablero.port.in.GetTableroByIdUseCase;
import com.ar.crm2.application.tablero.port.out.FindTableroByIdPort;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Tablero;
import com.ar.crm2.model.vo.TableroId;
import lombok.RequiredArgsConstructor;

/**
 * Application service implementing GetTableroByIdUseCase.
 * Loads a Tablero by id or throws TableroNotFoundException.
 */
@RequiredArgsConstructor
public class GetTableroByIdService implements GetTableroByIdUseCase {

    private final CrmAuthorization crmAuthorization;

    private final FindTableroByIdPort findPort;

    @Override
    public Tablero getById(GetTableroByIdCommand command) {
        TableroId tableroId = TableroId.from(command.id());
        crmAuthorization.requireRecord(RecursoCrm.TABLERO, AccionCrm.LEER, command.id());

        var tablero = findPort.findById(tableroId)
            .orElseThrow(() -> TableroNotFoundException.forId(command.id()));
        // The aggregate embeds column IDs, names, notes and WIP limits. Hide the
        // whole board rather than returning a partial or misleading aggregate.
        if (tablero.getColumnasTablero().stream().anyMatch(column ->
                !crmAuthorization.permitsRecord(RecursoCrm.COLUMNA, AccionCrm.LEER,
                    column.getColumnaId().value()))) {
            throw TableroNotFoundException.forId(command.id());
        }
        return tablero;
    }
}
