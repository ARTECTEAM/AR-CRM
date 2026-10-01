package com.ar.crm2.application.security.port.out;

import com.ar.crm2.model.autorizacion.AlcanceCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.application.security.ResourceScopeCandidate;
import com.ar.crm2.model.autorizacion.AccionCrm;

import java.util.Set;
import java.util.UUID;

/** Resource-owned evaluator for owner/assignee/board relationships; unsupported scopes fail closed. */
public interface ResourceScopePort {
    boolean supports(RecursoCrm resource);

    boolean permits(UUID usuarioId, RecursoCrm recurso, UUID recordId,
                    AlcanceCrm scope, Set<UUID> allowedIds);

    /** Action-aware scope check; legacy providers retain their existing policy by default. */
    default boolean permits(UUID usuarioId, RecursoCrm recurso, AccionCrm accion, UUID recordId,
                            AlcanceCrm scope, Set<UUID> allowedIds) {
        return permits(usuarioId, recurso, recordId, scope, allowedIds);
    }

    /** Candidate relationships default to deny until the resource adapter supports them explicitly. */
    default boolean permitsCandidate(UUID usuarioId, RecursoCrm recurso, ResourceScopeCandidate candidate,
                                     AlcanceCrm scope, Set<UUID> allowedIds) {
        return false;
    }
}
