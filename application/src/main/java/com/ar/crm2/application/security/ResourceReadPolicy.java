package com.ar.crm2.application.security;

import com.ar.crm2.model.autorizacion.AlcanceCrm;
import com.ar.crm2.model.autorizacion.GrupoCampoSensible;

import java.util.Set;
import java.util.UUID;

/** Immutable read/write field and row-scope projection for one resource. */
public record ResourceReadPolicy(
        AlcanceCrm scope,
        Set<GrupoCampoSensible> readableGroups,
        Set<UUID> allowedIds,
        Set<GrupoCampoSensible> writableGroups
) {
    public ResourceReadPolicy {
        if (scope == null) {
            throw new IllegalArgumentException("scope is required");
        }
        readableGroups = readableGroups == null ? Set.of() : Set.copyOf(readableGroups);
        allowedIds = allowedIds == null ? Set.of() : Set.copyOf(allowedIds);
        writableGroups = writableGroups == null ? Set.of() : Set.copyOf(writableGroups);
        if (!readableGroups.containsAll(writableGroups)) {
            throw new IllegalArgumentException("Writable field groups must also be readable");
        }
    }
}
