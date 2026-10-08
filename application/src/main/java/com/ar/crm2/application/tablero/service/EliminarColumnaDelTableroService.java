package com.ar.crm2.application.tablero.service;

import com.ar.crm2.application.security.CrmAuthorization;

import com.ar.crm2.application.tablero.command.EliminarColumnaDelTableroCommand;
import com.ar.crm2.application.tablero.exception.TableroNotFoundException;
import com.ar.crm2.application.ficha.port.out.ExistsFichasByColumnaIdPort;
import com.ar.crm2.application.tablero.port.in.EliminarColumnaDelTableroUseCase;
import com.ar.crm2.application.tablero.port.out.FindTableroByIdPort;
import com.ar.crm2.application.tablero.port.out.SaveTableroPort;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Tablero;
import com.ar.crm2.model.vo.ColumnaId;
import com.ar.crm2.model.vo.TableroId;
import lombok.RequiredArgsConstructor;

/**
 * Application service implementing EliminarColumnaDelTableroUseCase.
 *
 * <p>Flow:
 * <ol>
 *   <li>Load the Tablero aggregate via FindTableroByIdPort</li>
 *   <li>Check if the column contains fichas via ExistsFichasByColumnaIdPort</li>
 *   <li>Delegate to {@link Tablero#eliminarColumnaDelTablero} with the guard result</li>
 *   <li>Persist the updated aggregate via SaveTableroPort</li>
 * </ol>
 *
 * <p>Note: {@link com.ar.crm2.exception.ColumnaConFichasNoPuedeEliminarseException} thrown
 * by the domain will propagate if the column has fichas.
 */
@RequiredArgsConstructor
public class EliminarColumnaDelTableroService implements EliminarColumnaDelTableroUseCase {

    private final CrmAuthorization crmAuthorization;

    private final FindTableroByIdPort findPort;
    private final ExistsFichasByColumnaIdPort existsFichasPort;
    private final SaveTableroPort savePort;

    @Override
    public Tablero eliminarColumna(EliminarColumnaDelTableroCommand command) {
        TableroId tableroId = TableroId.from(command.tableroId());
        ColumnaId columnaId = ColumnaId.from(command.columnaId());
        crmAuthorization.requireRecord(RecursoCrm.TABLERO, AccionCrm.ACTUALIZAR, command.tableroId());
        crmAuthorization.requireRecord(RecursoCrm.COLUMNA, AccionCrm.LEER, command.columnaId());

        Tablero existing = findPort.findById(tableroId)
                .orElseThrow(() -> TableroNotFoundException.forId(command.tableroId()));

        // Guard: check if column has fichas before allowing deletion
        boolean tieneFichas = existsFichasPort.existsFichasByColumnaId(columnaId);

        // Domain enforces the business rule based on tieneFichas
        Tablero updated = existing.eliminarColumnaDelTablero(columnaId, tieneFichas);

        return savePort.save(updated);
    }
}
