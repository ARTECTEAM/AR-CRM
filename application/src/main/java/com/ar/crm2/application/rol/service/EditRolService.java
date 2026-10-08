package com.ar.crm2.application.rol.service;

import com.ar.crm2.application.rol.command.EditRolCommand;
import com.ar.crm2.application.rol.exception.RolNotFoundException;
import com.ar.crm2.application.rol.port.in.EditRolUseCase;
import com.ar.crm2.application.rol.port.out.FindRolByIdPort;
import com.ar.crm2.application.rol.port.out.SaveRolPort;
import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.application.security.CurrentActor;
import com.ar.crm2.application.security.exception.CrmActorUnavailableException;
import com.ar.crm2.application.security.exception.CrmAuthorizationDeniedException;
import com.ar.crm2.application.security.port.out.CurrentActorPort;
import com.ar.crm2.application.security.port.out.AuthorizationMutationPort;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Rol;
import com.ar.crm2.model.vo.RolId;
import lombok.RequiredArgsConstructor;

/**
 * Application service implementing EditRolUseCase.
 * Orchestrates loading the aggregate, applying the immutable domain update via reconstitute,
 * and saving. Preserves: id and activo.
 */
@RequiredArgsConstructor
public class EditRolService implements EditRolUseCase {

    private final FindRolByIdPort findPort;
    private final SaveRolPort savePort;
    private final CrmAuthorization authorization;
    private final CurrentActorPort currentActorPort;
    private final AuthorizationMutationPort mutationPort;

    @Override
    public Rol edit(EditRolCommand command) {
        return mutationPort.execute(() -> {
            authorization.require(RecursoCrm.ROL, AccionCrm.ADMINISTRAR);
            RolId rolId = RolId.from(command.id());

            Rol existing = findPort.findById(rolId)
                    .orElseThrow(() -> RolNotFoundException.forId(command.id()));

            CurrentActor actor = currentActorPort.currentActor()
                    .orElseThrow(() -> new CrmActorUnavailableException("No active CRM user is linked to this request"));
            boolean changingPower = command.permisos() != null
                    || (command.activo() != null && command.activo() != existing.isActivo());
            boolean expandingPower = command.permisos() != null
                    || (command.activo() != null && command.activo() && !existing.isActivo());
            if (changingPower && actor.rolId().equals(command.id())) {
                throw new CrmAuthorizationDeniedException("Users cannot change the permissions or active state of their own role");
            }

            Rol updated = Rol.reconstitute(
                    existing.getId(),
                    command.nombre(),
                    command.descripcion(),
                    command.activo() == null ? existing.isActivo() : command.activo(),
                    command.permisos() == null ? existing.getPermisos() : command.permisos()
            );

            if (expandingPower) {
                authorization.requireCanDelegateGrants(updated.getPermisos());
            }

            return savePort.save(updated);
        });
    }
}
