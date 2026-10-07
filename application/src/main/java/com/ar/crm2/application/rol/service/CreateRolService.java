package com.ar.crm2.application.rol.service;

import com.ar.crm2.application.rol.command.CreateRolCommand;
import com.ar.crm2.application.rol.port.in.CreateRolUseCase;
import com.ar.crm2.application.rol.port.out.SaveRolPort;
import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.application.security.port.out.AuthorizationMutationPort;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Rol;
import lombok.RequiredArgsConstructor;

/**
 * Application service implementing CreateRolUseCase.
 * Orchestrates domain entity creation and outbound persistence via SaveRolPort.
 * No Spring annotations — constructor injection via Lombok.
 */
@RequiredArgsConstructor
public class CreateRolService implements CreateRolUseCase {

    private final SaveRolPort savePort;
    private final CrmAuthorization authorization;
    private final AuthorizationMutationPort mutationPort;

    @Override
    public Rol create(CreateRolCommand command) {
        return mutationPort.execute(() -> {
            authorization.require(RecursoCrm.ROL, AccionCrm.ADMINISTRAR);
            authorization.requireCanDelegateGrants(command.permisos());
            Rol rol = Rol.create(
                command.nombre(),
                command.descripcion(),
                command.permisos()
            );
            return savePort.save(rol);
        });
    }
}
