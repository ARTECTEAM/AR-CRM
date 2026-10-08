package com.ar.crm2.application.usuario.service;

import com.ar.crm2.application.identity.port.out.SetIdentityEnabledPort;
import com.ar.crm2.application.identity.port.out.SyncIdentityEmailPort;
import com.ar.crm2.application.usuario.command.EditUsuarioCommand;
import com.ar.crm2.application.usuario.exception.UsuarioNotFoundException;
import com.ar.crm2.application.usuario.port.in.EditUsuarioUseCase;
import com.ar.crm2.application.usuario.port.out.FindUsuarioByIdPort;
import com.ar.crm2.application.usuario.port.out.PromoteBootstrapAdministratorPort;
import com.ar.crm2.application.usuario.port.out.SaveUsuarioPort;
import com.ar.crm2.application.rol.exception.RolNotFoundException;
import com.ar.crm2.application.rol.port.out.FindRolByIdPort;
import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.application.security.CurrentActor;
import com.ar.crm2.application.security.exception.CrmActorUnavailableException;
import com.ar.crm2.application.security.exception.CrmAuthorizationDeniedException;
import com.ar.crm2.application.security.port.out.CurrentActorPort;
import com.ar.crm2.application.security.port.out.AuthorizationMutationPort;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.AlcanceCrm;
import com.ar.crm2.model.autorizacion.GrupoCampoSensible;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Usuario;
import com.ar.crm2.model.vo.RolId;
import com.ar.crm2.model.vo.UsuarioId;
import lombok.RequiredArgsConstructor;

/**
 * Application service implementing EditUsuarioUseCase.
 * Orchestrates loading the aggregate, applying the immutable domain update via reconstitute,
 * and saving. Preserves database identity, creation time, enabled state, and the immutable Keycloak link.
 * Syncs email and enabled state to Keycloak before local update.
 * No Spring annotations — constructor injection via Lombok.
 */
@RequiredArgsConstructor
public class EditUsuarioService implements EditUsuarioUseCase {

    private final FindUsuarioByIdPort findPort;
    private final SaveUsuarioPort savePort;
    private final SyncIdentityEmailPort syncEmailPort;
    private final SetIdentityEnabledPort setEnabledPort;
    private final CrmAuthorization authorization;
    private final CurrentActorPort currentActorPort;
    private final FindRolByIdPort findRolByIdPort;
    private final PromoteBootstrapAdministratorPort promoteBootstrapAdministratorPort;
    private final AuthorizationMutationPort mutationPort;

    @Override
    public Usuario edit(EditUsuarioCommand command) {
        return mutationPort.execute(() -> editUnderLock(command));
    }

    private Usuario editUnderLock(EditUsuarioCommand command) {
        authorization.requireRecord(RecursoCrm.USUARIO, AccionCrm.ACTUALIZAR, command.id());
        UsuarioId usuarioId = UsuarioId.from(command.id());

        Usuario existing = findPort.findById(usuarioId)
                .orElseThrow(() -> UsuarioNotFoundException.forId(command.id()));

        var actor = currentActorPort.currentActor()
                .orElseThrow(() -> new CrmActorUnavailableException("No active CRM user is linked to this request"));
        RolId targetRoleId = command.rolId() == null ? existing.getRolId() : RolId.from(command.rolId());
        boolean changingRole = !existing.getRolId().equals(targetRoleId);
        boolean selfRoleChange = actor.usuarioId().equals(command.id());
        boolean bootstrapSelfPromotion = changingRole && selfRoleChange && actor.bootstrapAdmin();
        if (changingRole) {
            if (selfRoleChange && !actor.bootstrapAdmin()) {
                throw new CrmAuthorizationDeniedException("Users cannot change their own role assignment");
            }
            authorization.require(RecursoCrm.USUARIO, AccionCrm.ADMINISTRAR);
            var role = findRolByIdPort.findById(targetRoleId)
                    .orElseThrow(() -> RolNotFoundException.forId(targetRoleId.value()));
            if (!role.isActivo()) {
                throw new CrmActorUnavailableException("Cannot assign an inactive CRM role");
            }
            authorization.requireCanDelegateGrants(role.getPermisos());
            if (bootstrapSelfPromotion && role.getPermisos().stream().noneMatch(grant ->
                    grant.recurso() == RecursoCrm.ROL && grant.permite(AccionCrm.ADMINISTRAR)
                            && grant.alcance() == AlcanceCrm.TODO_COMPARTIDO)) {
                throw new CrmAuthorizationDeniedException(
                        "Bootstrap self-assignment only accepts a shared role-manager role");
            }
        }
        if (command.keycloakId() != null && !command.keycloakId().equals(existing.getKeycloakId())) {
            throw new CrmAuthorizationDeniedException("Keycloak identity cannot be changed through profile editing");
        }
        if (!command.correo().equals(existing.getCorreo())) {
            authorization.requireWritableGroups(RecursoCrm.USUARIO, java.util.Set.of(GrupoCampoSensible.CONTACTO_PRIVADO));
        }

        String keycloakId = existing.getKeycloakId();

        // Keep the shared role lock through external sync and local persistence
        // so role/assignment authorization cannot change before this commit.
        // This intentionally trades write throughput for an atomic first slice.
        // Sync email to Keycloak first — if this fails, do not update local
        if (!command.correo().equals(existing.getCorreo()) && keycloakId != null) {
            syncEmailPort.syncEmail(keycloakId, command.correo());
        }

        // Sync enabled flag to Keycloak
        if (keycloakId != null) {
            setEnabledPort.setEnabled(keycloakId, existing.isActivo());
        }

        Usuario updated = Usuario.reconstitute(
                existing.getId(),
                command.nombre(),
                command.correo(),
                targetRoleId,
                existing.getCreadoEn(),
                existing.isActivo(),
                keycloakId
        );

        if (bootstrapSelfPromotion) {
            return promoteBootstrapAdministratorPort.promoteIfNoActiveManager(updated, actor.usuarioId());
        }
        return savePort.save(updated);
    }
}
