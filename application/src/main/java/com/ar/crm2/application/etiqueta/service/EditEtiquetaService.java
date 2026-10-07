package com.ar.crm2.application.etiqueta.service;

import com.ar.crm2.application.etiqueta.command.EditEtiquetaCommand;
import com.ar.crm2.application.etiqueta.exception.EtiquetaNotFoundException;
import com.ar.crm2.application.etiqueta.port.in.EditEtiquetaUseCase;
import com.ar.crm2.application.etiqueta.port.out.ExistsEtiquetaByNombreAndTipoPort;
import com.ar.crm2.application.etiqueta.port.out.FindEtiquetaByIdPort;
import com.ar.crm2.application.etiqueta.port.out.SaveEtiquetaPort;
import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.exception.DuplicateEtiquetaNameException;
import com.ar.crm2.model.entity.Etiqueta;
import com.ar.crm2.model.vo.EtiquetaId;
import lombok.RequiredArgsConstructor;

/**
 * Application service for editing an existing Etiqueta.
 *
 * <p>Enforces (nombre, tipoEtiqueta) uniqueness on rename: if the new name
 * collides with another Etiqueta of the same type, the operation is rejected
 * with {@link DuplicateEtiquetaNameException}. The current entity's id is
 * passed as {@code excludeId} so the port can ignore the row being edited —
 * a realistic JPA {@code existsByNombreAndTipo} adapter would otherwise
 * match the row to itself and reject valid recolor / idempotent rename
 * operations.
 *
 * <p>Note: Transaction boundary is owned by the infrastructure adapter.
 */
@RequiredArgsConstructor
public class EditEtiquetaService implements EditEtiquetaUseCase {

    private final FindEtiquetaByIdPort findPort;
    private final SaveEtiquetaPort savePort;
    private final ExistsEtiquetaByNombreAndTipoPort existsPort;
    private final CrmAuthorization authorization;

    @Override
    public Etiqueta edit(EditEtiquetaCommand command) {
        authorization.require(RecursoCrm.ETIQUETA, AccionCrm.ACTUALIZAR);
        authorization.requireRecord(RecursoCrm.ETIQUETA, AccionCrm.ACTUALIZAR, command.id());
        EtiquetaId id = EtiquetaId.from(command.id());
        Etiqueta existing = findPort.findById(id)
            .orElseThrow(() -> EtiquetaNotFoundException.forId(command.id()));

        if (existsPort.exists(command.nombre(), existing.getTipoEtiqueta(), id)) {
            throw new DuplicateEtiquetaNameException();
        }

        Etiqueta renamed = existing.rename(command.nombre());
        Etiqueta updated = renamed.recolor(command.color());

        return savePort.save(updated);
    }
}
