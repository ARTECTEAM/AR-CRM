package com.ar.crm2.application.columna.service;

import com.ar.crm2.application.security.CrmAuthorization;

import com.ar.crm2.application.columna.command.EditColumnaCommand;
import com.ar.crm2.application.columna.exception.ColumnaNotFoundException;
import com.ar.crm2.application.columna.port.in.EditColumnaUseCase;
import com.ar.crm2.application.columna.port.out.FindAllColumnasPort;
import com.ar.crm2.application.columna.port.out.FindColumnaByIdPort;
import com.ar.crm2.application.columna.port.out.SaveColumnaPort;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Columna;
import com.ar.crm2.model.vo.ColumnaId;
import lombok.RequiredArgsConstructor;

/**
 * Application service implementing EditColumnaUseCase.
 *
 * <p>Coordination responsibility only:
 * <ol>
 *   <li>Load the existing Columna via {@link FindColumnaByIdPort}.</li>
 *   <li>Scan the catalog through {@link FindAllColumnasPort}.</li>
 *   <li>Delegate duplicate-scope evaluation to
 *       {@link Columna#hasDuplicateForEdit(java.util.List, ColumnaId, com.ar.crm2.model.enums.TipoTablero, String)}.</li>
 *   <li>Delegate the update to {@link Columna#reconstitute}.</li>
 *   <li>Persist via {@link SaveColumnaPort}.</li>
 * </ol>
 *
 * <p>The previous implementation delegated duplicate evaluation to the
 * now-removed {@code ColumnaNamePolicy} application helper. Duplicate
 * detection is a domain rule (the catalog identity lives in
 * {@link Columna}) and now lives in {@link Columna#hasDuplicateForEdit}.
 */
@RequiredArgsConstructor
public class EditColumnaService implements EditColumnaUseCase {

    private final CrmAuthorization crmAuthorization;

    private final FindColumnaByIdPort findPort;
    private final FindAllColumnasPort findAllPort;
    private final SaveColumnaPort savePort;

    @Override
    public Columna edit(EditColumnaCommand command) {
        ColumnaId columnaId = ColumnaId.from(command.id());
        crmAuthorization.requireRecord(RecursoCrm.COLUMNA, AccionCrm.ACTUALIZAR, command.id());

        Columna existing = findPort.findById(columnaId)
                .orElseThrow(() -> ColumnaNotFoundException.forId(command.id()));

        boolean existeDuplicado = Columna.hasDuplicateForEdit(
            findAllPort.findAll(),
            columnaId,
            command.tipoTablero(),
            command.nombre()
        );

        Columna updated = Columna.reconstitute(
                existing.getId(),
                command.nombre(),
                command.color(),
                command.tipoTablero(),
                command.tipoColumna(),
                existeDuplicado
        );

        return savePort.save(updated);
    }
}
