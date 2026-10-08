package com.ar.crm2.application.contacto.service;

import com.ar.crm2.application.contacto.command.DeleteContactoCommand;
import com.ar.crm2.application.contacto.exception.ContactoHasAssociatedTratosException;
import com.ar.crm2.application.contacto.exception.ContactoNotFoundException;
import com.ar.crm2.application.contacto.port.in.DeleteContactoUseCase;
import com.ar.crm2.application.contacto.port.out.DeleteContactoByIdPort;
import com.ar.crm2.application.contacto.port.out.ExistsTratosByContactoIdPort;
import com.ar.crm2.application.contacto.port.out.FindContactoByIdPort;
import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.vo.ContactoId;
import lombok.RequiredArgsConstructor;

/**
 * Application service implementing DeleteContactoUseCase.
 * Validates existence and business invariants before hard-deleting.
 */
@RequiredArgsConstructor
public class DeleteContactoService implements DeleteContactoUseCase {

    private final FindContactoByIdPort findPort;
    private final ExistsTratosByContactoIdPort existsTratosPort;
    private final DeleteContactoByIdPort deletePort;
    private final CrmAuthorization authorization;

    @Override
    public void delete(DeleteContactoCommand command) {
        authorization.require(RecursoCrm.CONTACTO, AccionCrm.ELIMINAR);
        authorization.requireRecord(RecursoCrm.CONTACTO, AccionCrm.ELIMINAR, command.id());
        ContactoId contactoId = ContactoId.from(command.id());

        // Verify contacto exists
        findPort.findById(contactoId)
                .orElseThrow(() -> ContactoNotFoundException.forId(command.id()));

        // Guard: cannot delete if associated Tratos exist
        if (existsTratosPort.existsTratosByContactoId(contactoId)) {
            throw ContactoHasAssociatedTratosException.forId(command.id());
        }

        deletePort.deleteById(contactoId);
    }
}
