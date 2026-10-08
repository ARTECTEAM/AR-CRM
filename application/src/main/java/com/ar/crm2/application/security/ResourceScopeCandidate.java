package com.ar.crm2.application.security;

import com.ar.crm2.model.autorizacion.RecursoCrm;

import java.util.UUID;

/** Candidate relationship data needed to authorize a row before it is persisted. */
public record ResourceScopeCandidate(
        RecursoCrm targetResource,
        UUID targetId,
        UUID responsibleUserId,
        UUID creatorUserId
) {
    public ResourceScopeCandidate {
        if ((targetResource == null) != (targetId == null)) {
            throw new IllegalArgumentException("targetResource and targetId must be set together");
        }
    }
}
