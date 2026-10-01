package com.ar.crm2.application.usuario.service;

import com.ar.crm2.application.identity.port.out.DeleteIdentityPort;
import com.ar.crm2.application.usuario.command.DeleteUsuarioCommand;
import com.ar.crm2.application.usuario.exception.UsuarioNotFoundException;
import com.ar.crm2.application.usuario.port.in.DeleteUsuarioUseCase;
import com.ar.crm2.application.usuario.port.out.DeleteUsuarioByIdPort;
import com.ar.crm2.application.usuario.port.out.FindUsuarioByIdPort;
import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.application.security.port.out.CurrentActorPort;
import com.ar.crm2.application.security.port.out.AuthorizationMutationPort;
import com.ar.crm2.application.security.exception.CrmActorUnavailableException;
import com.ar.crm2.application.security.exception.CrmAuthorizationDeniedException;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.vo.UsuarioId;
import lombok.RequiredArgsConstructor;

/**
 * Application service implementing DeleteUsuarioUseCase.
 * <p>
 * Consistency model: the local DB is the source of truth, so the local row is
 * deleted first and the Keycloak identity is removed as a best-effort follow-up.
 * <p>
 * Flow:
 * <ol>
 *   <li>Look up the local usuario to obtain its {@code keycloakId} (and to
 *       surface {@link UsuarioNotFoundException} when it does not exist).</li>
 *   <li>Delete the local DB row. If this fails, the Keycloak identity is left
 *       untouched — the operator can retry the delete without producing a
 *       stale Keycloak user with no CRM counterpart.</li>
 *   <li>If a {@code keycloakId} is present, call
 *       {@link DeleteIdentityPort#delete(String)} as best-effort cleanup. A
 *       failure here is swallowed and the method still returns success:
 *       orphaning a Keycloak identity is strictly preferable to blocking the
 *       user delete, and the orphan can be reconciled by an operator later.</li>
 * </ol>
 * No Spring annotations — constructor injection via Lombok.
 */
@RequiredArgsConstructor
public class DeleteUsuarioService implements DeleteUsuarioUseCase {

    private final FindUsuarioByIdPort findPort;
    private final DeleteUsuarioByIdPort deletePort;
    private final DeleteIdentityPort deleteIdentityPort;
    private final CrmAuthorization authorization;
    private final CurrentActorPort currentActorPort;
    private final AuthorizationMutationPort mutationPort;

    @Override
    public void delete(DeleteUsuarioCommand command) {
        String keycloakId = mutationPort.execute(() -> {
            authorization.require(RecursoCrm.USUARIO, AccionCrm.ADMINISTRAR);
            var actor = currentActorPort.currentActor()
                    .orElseThrow(() -> new CrmActorUnavailableException("No active CRM user is linked to this request"));
            if (actor.usuarioId().equals(command.id())) {
                throw new CrmAuthorizationDeniedException("Users cannot delete their own CRM account");
            }
            UsuarioId usuarioId = UsuarioId.from(command.id());

            String linkedIdentity = findPort.findById(usuarioId)
                    .orElseThrow(() -> UsuarioNotFoundException.forId(command.id()))
                    .getKeycloakId();

            // The local deletion and its actor/assignment checks share the role lock.
            deletePort.deleteById(usuarioId);
            return linkedIdentity;
        });

        // Best-effort Keycloak cleanup. The local delete is already committed,
        // so any failure here is recorded as an orphan identity and must not
        // cause the caller's delete to fail.
        if (keycloakId != null) {
            try {
                deleteIdentityPort.delete(keycloakId);
            } catch (RuntimeException keycloakDeleteFailure) {
                // Intentionally swallowed: the local delete stands, and the
                // orphan Keycloak identity is reconciled out-of-band.
            }
        }
    }
}
