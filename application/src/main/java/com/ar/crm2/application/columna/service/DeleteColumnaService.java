package com.ar.crm2.application.columna.service;

import com.ar.crm2.application.security.CrmAuthorization;

import com.ar.crm2.application.columna.command.DeleteColumnaCommand;
import com.ar.crm2.application.columna.exception.ColumnaHasAssociatedFichasException;
import com.ar.crm2.application.columna.exception.ColumnaNotFoundException;
import com.ar.crm2.application.columna.port.in.DeleteColumnaUseCase;
import com.ar.crm2.application.columna.port.out.DeleteColumnaByIdPort;
import com.ar.crm2.application.columna.port.out.ExistsColumnaAsignadaPort;
import com.ar.crm2.application.columna.port.out.FindColumnaByIdPort;
import com.ar.crm2.application.ficha.port.out.ExistsFichasByColumnaIdPort;
import com.ar.crm2.exception.ColumnaEnUsoNoPuedeEliminarseException;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.vo.ColumnaId;
import lombok.RequiredArgsConstructor;

/**
 * Application service implementing DeleteColumnaUseCase.
 * Validates existence, board assignment, and Ficha associations before hard-deleting
 * a catalog Columna.
 */
@RequiredArgsConstructor
public class DeleteColumnaService implements DeleteColumnaUseCase {

    private final CrmAuthorization crmAuthorization;

    private final FindColumnaByIdPort findPort;
    private final ExistsColumnaAsignadaPort existsAsignadaPort;
    private final ExistsFichasByColumnaIdPort existsFichasPort;
    private final DeleteColumnaByIdPort deletePort;

    @Override
    public void delete(DeleteColumnaCommand command) {
        ColumnaId columnaId = ColumnaId.from(command.id());
        crmAuthorization.requireRecord(RecursoCrm.COLUMNA, AccionCrm.ELIMINAR, command.id());

        // Verify columna exists in catalog
        findPort.findById(columnaId)
                .orElseThrow(() -> ColumnaNotFoundException.forId(command.id()));

        // Guard against deleting a column that is still assigned to any board
        if (existsAsignadaPort.existsByColumnaId(columnaId)) {
            throw new ColumnaEnUsoNoPuedeEliminarseException();
        }

        // Check for associated fichas
        if (existsFichasPort.existsFichasByColumnaId(columnaId)) {
            throw ColumnaHasAssociatedFichasException.forId(command.id());
        }

        deletePort.deleteById(columnaId);
    }
}
