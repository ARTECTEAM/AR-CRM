package com.ar.crm2.application.security;

import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.AlcanceCrm;
import com.ar.crm2.model.autorizacion.GrupoCampoSensible;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** Coarse resource-grant summary; deliberately excludes record identifiers. */
public record ResourceCapabilities(
        AlcanceCrm scope,
        Set<AccionCrm> actions,
        Set<GrupoCampoSensible> readableGroups,
        Set<GrupoCampoSensible> writableGroups
) {
    public ResourceCapabilities {
        if (scope == null) {
            throw new IllegalArgumentException("scope is required");
        }
        actions = immutableEnumSet(actions, AccionCrm.class);
        readableGroups = immutableEnumSet(readableGroups, GrupoCampoSensible.class);
        writableGroups = immutableEnumSet(writableGroups, GrupoCampoSensible.class);
    }

    public boolean permits(AccionCrm action) {
        return action != null && actions.contains(action);
    }

    private static <E extends Enum<E>> Set<E> immutableEnumSet(Set<E> values, Class<E> type) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        EnumSet<E> copy = EnumSet.noneOf(type);
        copy.addAll(values);
        return Collections.unmodifiableSet(copy);
    }
}
