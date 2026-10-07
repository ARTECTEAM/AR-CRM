package com.ar.crm2.application.tablero.service;

import com.ar.crm2.application.security.CrmAuthorization;

import com.ar.crm2.application.tablero.command.ReordenarColumnasCommand;
import com.ar.crm2.application.tablero.exception.TableroNotFoundException;
import com.ar.crm2.application.tablero.port.in.ReordenarColumnasUseCase;
import com.ar.crm2.application.tablero.port.out.FindTableroByIdPort;
import com.ar.crm2.application.tablero.port.out.SaveTableroPort;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Tablero;
import com.ar.crm2.model.vo.TableroId;
import lombok.RequiredArgsConstructor;

/**
 * Application service implementing ReordenarColumnasUseCase.
 * Loads the Tablero aggregate, invokes domain reordering behavior,
 * and persists the updated aggregate.
 */
@RequiredArgsConstructor
public class ReordenarColumnasService implements ReordenarColumnasUseCase {

    private final CrmAuthorization crmAuthorization;

    private final FindTableroByIdPort findPort;
    private final SaveTableroPort savePort;

    @Override
    public Tablero reordenar(ReordenarColumnasCommand command) {
        TableroId tableroId = TableroId.from(command.tableroId());
        crmAuthorization.requireRecord(RecursoCrm.TABLERO, AccionCrm.ACTUALIZAR, command.tableroId());
        if (command.nuevoOrden() != null) {
            command.nuevoOrden().forEach(id -> crmAuthorization.requireRecord(
                    RecursoCrm.COLUMNA, AccionCrm.LEER, id.value()));
        }

        Tablero existing = findPort.findById(tableroId)
                .orElseThrow(() -> TableroNotFoundException.forId(command.tableroId()));

        Tablero updated = existing.reordenarColumnas(command.nuevoOrden());

        return savePort.save(updated);
    }
}
