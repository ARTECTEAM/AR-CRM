package com.ar.crm2.application.rol.service;

import com.ar.crm2.application.rol.command.DeleteRolCommand;
import com.ar.crm2.application.rol.exception.RolHasAssociatedUsuariosException;
import com.ar.crm2.application.rol.exception.RolNotFoundException;
import com.ar.crm2.application.rol.port.in.DeleteRolUseCase;
import com.ar.crm2.application.rol.port.out.DeleteRolByIdPort;
import com.ar.crm2.application.rol.port.out.ExistsUsuariosByRolIdPort;
import com.ar.crm2.application.rol.port.out.FindRolByIdPort;
import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.application.security.port.out.AuthorizationMutationPort;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.vo.RolId;
import lombok.RequiredArgsConstructor;

/**
 * Application service implementing DeleteRolUseCase.
 * Validates existence and usuario associations before hard-deleting.
 */
@RequiredArgsConstructor
public class DeleteRolService implements DeleteRolUseCase {

    private final FindRolByIdPort findPort;
    private final ExistsUsuariosByRolIdPort existsUsuariosPort;
    private final DeleteRolByIdPort deletePort;
    private final CrmAuthorization authorization;
    private final AuthorizationMutationPort mutationPort;

    @Override
    public void delete(DeleteRolCommand command) {
        mutationPort.execute(() -> {
            authorization.require(RecursoCrm.ROL, AccionCrm.ADMINISTRAR);
            RolId rolId = RolId.from(command.id());

            findPort.findById(rolId)
                    .orElseThrow(() -> RolNotFoundException.forId(command.id()));

            if (existsUsuariosPort.existsUsuariosByRolId(rolId)) {
                throw RolHasAssociatedUsuariosException.forId(command.id());
            }

            deletePort.deleteById(rolId);
            return null;
        });
    }
}
