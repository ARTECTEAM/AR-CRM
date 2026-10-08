package com.ar.crm2.application.security;

import java.util.UUID;

/** Current active CRM user and role, resolved from the authenticated JWT subject on demand. */
public record CurrentActor(UUID usuarioId, UUID rolId, boolean bootstrapAdmin) {
    public CurrentActor {
        if (usuarioId == null || rolId == null) {
            throw new IllegalArgumentException("An active CRM user and role are required");
        }
    }
}
