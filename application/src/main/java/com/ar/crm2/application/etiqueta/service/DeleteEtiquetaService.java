package com.ar.crm2.application.etiqueta.service;

import com.ar.crm2.application.etiqueta.command.DeleteEtiquetaCommand;
import com.ar.crm2.application.etiqueta.exception.EtiquetaNotFoundException;
import com.ar.crm2.application.etiqueta.exception.EtiquetaRequiresConfirmationException;
import com.ar.crm2.application.etiqueta.port.in.DeleteEtiquetaUseCase;
import com.ar.crm2.application.etiqueta.port.out.CountFichaEtiquetasByEtiquetaIdPort;
import com.ar.crm2.application.etiqueta.port.out.DeleteEtiquetaByIdPort;
import com.ar.crm2.application.etiqueta.port.out.DeleteFichaEtiquetasByEtiquetaIdPort;
import com.ar.crm2.application.etiqueta.port.out.FindEtiquetaByIdPort;
import com.ar.crm2.application.etiqueta.port.out.FindFichaIdsByEtiquetaIdPort;
import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Etiqueta;
import com.ar.crm2.model.vo.EtiquetaId;
import lombok.RequiredArgsConstructor;

/**
 * Application service for deleting an Etiqueta.
 *
 * <p>When the Etiqueta is referenced by one or more FichaEtiqueta rows,
 * the caller must pass {@code confirm=true} or the call is rejected
 * with {@link EtiquetaRequiresConfirmationException}.
 *
 * <p>On confirmed delete: the FichaEtiqueta rows are removed first, then
 * the catalog row. The infrastructure adapter must wrap this call in a
 * single transaction so the two deletes are atomic.
 */
@RequiredArgsConstructor
public class DeleteEtiquetaService implements DeleteEtiquetaUseCase {

    private final FindEtiquetaByIdPort findPort;
    private final CountFichaEtiquetasByEtiquetaIdPort countPort;
    private final DeleteFichaEtiquetasByEtiquetaIdPort deleteRelPort;
    private final DeleteEtiquetaByIdPort deleteByIdPort;
    private final FindFichaIdsByEtiquetaIdPort findFichaIdsPort;
    private final CrmAuthorization authorization;

    @Override
    public void delete(DeleteEtiquetaCommand command) {
        authorization.require(RecursoCrm.ETIQUETA, AccionCrm.ELIMINAR);
        authorization.requireRecord(RecursoCrm.ETIQUETA, AccionCrm.ELIMINAR, command.id());
        EtiquetaId id = EtiquetaId.from(command.id());
        Etiqueta existing = findPort.findById(id)
            .orElseThrow(() -> EtiquetaNotFoundException.forId(command.id()));

        // A global label delete also removes its links from each Ficha.
        // Authorize every affected row before exposing usage through the
        // confirmation behavior or mutating any relationship.
        for (var fichaId : findFichaIdsPort.findFichaIdsByEtiquetaId(id)) {
            authorization.requireRecord(RecursoCrm.FICHA, AccionCrm.ACTUALIZAR, fichaId);
        }

        long relationCount = countPort.countByEtiquetaId(existing.getId());
        if (relationCount > 0 && !command.confirm()) {
            throw new EtiquetaRequiresConfirmationException();
        }

        deleteRelPort.deleteByEtiquetaId(existing.getId());
        deleteByIdPort.deleteById(existing.getId());
    }
}
