package com.ar.crm2.application.tablero.service;

import com.ar.crm2.application.security.CrmAuthorization;

import com.ar.crm2.application.tablero.command.EditTableroCommand;
import com.ar.crm2.application.tablero.exception.TableroNotFoundException;
import com.ar.crm2.application.tablero.port.in.EditTableroUseCase;
import com.ar.crm2.application.tablero.port.out.FindTableroByIdPort;
import com.ar.crm2.application.tablero.port.out.SaveTableroPort;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Tablero;
import com.ar.crm2.model.vo.TableroId;
import lombok.RequiredArgsConstructor;

/**
 * Application service implementing EditTableroUseCase.
 *
 * <p>Coordination responsibility only:
 * <ol>
 *   <li>Load the existing Tablero aggregate via {@link FindTableroByIdPort}.</li>
 *   <li>Delegate the edit to the domain via {@link Tablero#editarDatos(String, String)}.</li>
 *   <li>Persist the updated aggregate via {@link SaveTableroPort}.</li>
 * </ol>
 *
 * <p>The previous implementation reconstructed the aggregate through
 * {@code Tablero.reconstitute(...)} for edits, conflating persistence
 * hydration with edit semantics. The new flow uses the domain's explicit
 * edit behavior so that {@code reconstitute} is reserved for the persistence
 * layer only.
 */
@RequiredArgsConstructor
public class EditTableroService implements EditTableroUseCase {

    private final CrmAuthorization crmAuthorization;

    private final FindTableroByIdPort findPort;
    private final SaveTableroPort savePort;

    @Override
    public Tablero edit(EditTableroCommand command) {
        TableroId tableroId = TableroId.from(command.id());
        crmAuthorization.requireRecord(RecursoCrm.TABLERO, AccionCrm.ACTUALIZAR, command.id());

        Tablero existing = findPort.findById(tableroId)
                .orElseThrow(() -> TableroNotFoundException.forId(command.id()));

        Tablero updated = existing.editarDatos(command.nombre(), command.descripcion());

        return savePort.save(updated);
    }
}
