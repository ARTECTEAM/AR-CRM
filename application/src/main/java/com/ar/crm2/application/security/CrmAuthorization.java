package com.ar.crm2.application.security;

import com.ar.crm2.application.security.exception.CrmAuthorizationDeniedException;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.GrupoCampoSensible;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.autorizacion.PermisoRecurso;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

/** Application boundary for action, record-scope, and sensitive-field authorization. */
public interface CrmAuthorization {
    /**
     * Returns a coarse role-level snapshot for model tool discovery. It is
     * only an upper bound; record and field access must still be checked by
     * the authoritative application use case on every invocation.
     */
    default AuthorizationCapabilities authorizationCapabilities() {
        return AuthorizationCapabilities.none();
    }

    void require(RecursoCrm recurso, AccionCrm accion);

    boolean permitsRecord(RecursoCrm recurso, AccionCrm accion, UUID recordId);

    void requireRecord(RecursoCrm recurso, AccionCrm accion, UUID recordId);

    /** Authorizes proposed relationship fields before creating or changing a row. */
    default void requireCandidate(RecursoCrm recurso, AccionCrm accion, ResourceScopeCandidate candidate) {
        throw new CrmAuthorizationDeniedException("Candidate authorization is not supported for " + recurso);
    }

    /** Prevents role management and assignments from granting beyond the caller's current authority. */
    default void requireCanDelegateGrants(Collection<PermisoRecurso> requestedGrants) {
        throw new CrmAuthorizationDeniedException("Permission delegation is not supported");
    }

    ResourceReadPolicy readPolicy(RecursoCrm recurso);

    /** Field-only policy for projecting a write result; does not grant resource read access. */
    ResourceReadPolicy fieldPolicy(RecursoCrm recurso);

    void requireWritableGroups(RecursoCrm recurso, Set<GrupoCampoSensible> groups);

    /** Deterministic opaque digest of the active local user's current grants. */
    String revision();
}
