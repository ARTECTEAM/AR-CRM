package com.ar.crm2.application.empresa.service;

import com.ar.crm2.application.empresa.command.DeleteEmpresaCommand;
import com.ar.crm2.application.empresa.exception.EmpresaHasAssociatedTratosException;
import com.ar.crm2.application.empresa.exception.EmpresaNotFoundException;
import com.ar.crm2.application.empresa.port.in.DeleteEmpresaUseCase;
import com.ar.crm2.application.empresa.port.out.DeleteEmpresaByIdPort;
import com.ar.crm2.application.empresa.port.out.ExistsTratosByEmpresaIdPort;
import com.ar.crm2.application.empresa.port.out.FindEmpresaByIdPort;
import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.vo.EmpresaId;
import lombok.RequiredArgsConstructor;

/**
 * Application service implementing DeleteEmpresaUseCase.
 * Validates existence and business invariants before hard-deleting.
 */
@RequiredArgsConstructor
public class DeleteEmpresaService implements DeleteEmpresaUseCase {

    private final FindEmpresaByIdPort findPort;
    private final ExistsTratosByEmpresaIdPort existsTratosPort;
    private final DeleteEmpresaByIdPort deletePort;
    private final CrmAuthorization authorization;

    @Override
    public void delete(DeleteEmpresaCommand command) {
        authorization.require(RecursoCrm.EMPRESA, AccionCrm.ELIMINAR);
        authorization.requireRecord(RecursoCrm.EMPRESA, AccionCrm.ELIMINAR, command.id());
        EmpresaId empresaId = EmpresaId.from(command.id());

        // Verify empresa exists
        findPort.findById(empresaId)
                .orElseThrow(() -> EmpresaNotFoundException.forId(command.id()));

        // Guard: cannot delete if associated Tratos exist
        if (existsTratosPort.existsTratosByEmpresaId(empresaId)) {
            throw EmpresaHasAssociatedTratosException.forId(command.id());
        }

        deletePort.deleteById(empresaId);
    }
}
