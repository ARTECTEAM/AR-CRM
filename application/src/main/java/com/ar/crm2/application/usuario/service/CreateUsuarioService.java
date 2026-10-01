package com.ar.crm2.application.usuario.service;

import com.ar.crm2.application.identity.port.out.DeleteIdentityPort;
import com.ar.crm2.application.identity.port.out.ProvisionIdentityPort;
import com.ar.crm2.application.identity.port.out.SetIdentityAttributesPort;
import com.ar.crm2.application.usuario.command.CreateUsuarioCommand;
import com.ar.crm2.application.usuario.port.in.CreateUsuarioUseCase;
import com.ar.crm2.application.usuario.port.out.SaveUsuarioPort;
import com.ar.crm2.application.rol.exception.RolNotFoundException;
import com.ar.crm2.application.rol.port.out.FindRolByIdPort;
import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.application.security.CurrentActor;
import com.ar.crm2.application.security.exception.CrmActorUnavailableException;
import com.ar.crm2.application.security.port.out.CurrentActorPort;
import com.ar.crm2.application.security.port.out.AuthorizationMutationPort;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Usuario;
import com.ar.crm2.model.vo.RolId;
import lombok.RequiredArgsConstructor;

import java.util.Map;

/**
 * Application service implementing CreateUsuarioUseCase.
 * <p>
 * Consistency model: Keycloak is provisioned first, then the locally
 * generated {@code usuario_id} is pushed to Keycloak as a custom user
 * attribute (consumed by the configured protocol mapper to emit the
 * {@code usuario_id} JWT claim) BEFORE the local row is persisted. This keeps
 * Keycloak and the local DB in lockstep: if either external step fails, the
 * whole flow is rolled back via compensation deletes, and the caller observes
 * a single, attributable failure.
 * <p>
 * Flow:
 * <ol>
 *   <li>Provision the Keycloak user (fails fast on connection / auth issues).</li>
 *   <li>Build the local {@link Usuario} with the provisioned {@code keycloakId}.
 *       {@code Usuario.create(...)} generates the {@code usuario_id} now, so
 *       it is available for the next step.</li>
 *   <li>Push {@code usuario_id} to Keycloak via
 *       {@link SetIdentityAttributesPort#setAttributes(String, Map)}.
 *       If this fails, the Keycloak user is deleted as compensation, the
 *       local row is never saved, and the original failure is rethrown.</li>
 *   <li>Persist the local row. If this fails after the attribute sync, the
 *       Keycloak user is deleted as compensation and the original save
 *       failure is rethrown — leaving a Keycloak user without a CRM
 *       counterpart would leave the JWT {@code usuario_id} claim dangling.</li>
 * </ol>
 * Compensation failures (Keycloak delete itself failing) are swallowed and
 * do not mask the original error.
 * <p>
 * No Spring annotations — constructor injection via Lombok.
 */
@RequiredArgsConstructor
public class CreateUsuarioService implements CreateUsuarioUseCase {

    private final SaveUsuarioPort savePort;
    private final ProvisionIdentityPort provisionPort;
    private final DeleteIdentityPort deleteIdentityPort;
    private final SetIdentityAttributesPort setAttributesPort;
    private final CrmAuthorization authorization;
    private final CurrentActorPort currentActorPort;
    private final FindRolByIdPort findRolByIdPort;
    private final AuthorizationMutationPort mutationPort;

    @Override
    public Usuario create(CreateUsuarioCommand command) {
        validateAssignment(command);

        // The preflight prevents unnecessary identity-provider work, but its
        // snapshots are never trusted for the local assignment below.
        RolId targetRolId = RolId.from(command.rolId());

        // Provision Keycloak first — we need keycloakId before we can build the
        // local Usuario entity, and a Keycloak failure must short-circuit
        // before any local work happens.
        var provisioned = provisionPort.provision(
            command.correo(),
            command.initialPassword(),
            true // enabled
        );

        // Build the local entity up front so the generated usuario_id is
        // available for the Keycloak attribute sync that follows.
        Usuario usuario = Usuario.create(
            command.nombre(),
            command.correo(),
            targetRolId,
            provisioned.keycloakId()
        );

        // Sync usuario_id to Keycloak BEFORE the local save. If this fails,
        // the Keycloak user is rolled back and the original failure is surfaced.
        try {
            setAttributesPort.setAttributes(
                provisioned.keycloakId(),
                Map.of("usuario_id", usuario.getId().value().toString())
            );
        } catch (RuntimeException syncFailure) {
            compensateIdentity(provisioned.keycloakId());
            throw syncFailure;
        }

        try {
            return mutationPort.execute(() -> {
                // Re-read actor and target role after the ordered role lock is held.
                validateAssignment(command);
                return savePort.save(usuario);
            });
        } catch (RuntimeException saveFailure) {
            // Late authorization denial and persistence failure both leave no
            // local CRM user; remove the already-provisioned identity.
            compensateIdentity(provisioned.keycloakId());
            throw saveFailure;
        }
    }

    private void validateAssignment(CreateUsuarioCommand command) {
        authorization.require(RecursoCrm.USUARIO, AccionCrm.CREAR);
        CurrentActor actor = currentActorPort.currentActor()
                .orElseThrow(() -> new CrmActorUnavailableException("No active CRM user is linked to this request"));
        RolId targetRolId = RolId.from(command.rolId());
        var targetRole = findRolByIdPort.findById(targetRolId)
                .orElseThrow(() -> RolNotFoundException.forId(command.rolId()));
        if (!targetRole.isActivo()) {
            throw new CrmActorUnavailableException("Cannot assign an inactive CRM role");
        }
        authorization.requireCanDelegateGrants(targetRole.getPermisos());
        if (!actor.rolId().equals(command.rolId())) {
            authorization.require(RecursoCrm.USUARIO, AccionCrm.ADMINISTRAR);
        }
    }

    private void compensateIdentity(String keycloakId) {
        try {
            deleteIdentityPort.delete(keycloakId);
        } catch (RuntimeException compensationFailure) {
            // Preserve the original failure; an orphan identity can be reconciled by an operator.
        }
    }
}
