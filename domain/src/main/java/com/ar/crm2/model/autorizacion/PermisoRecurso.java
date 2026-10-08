package com.ar.crm2.model.autorizacion;

import com.ar.crm2.shared.DomainAssert;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * One editable policy row per resource. Actions, row scope, allowed board IDs, and
 * sensitive field groups are drawn from fixed catalogs rather than executable rules.
 */
public record PermisoRecurso(
        RecursoCrm recurso,
        Set<AccionCrm> acciones,
        AlcanceCrm alcance,
        Set<UUID> idsPermitidos,
        Set<GrupoCampoSensible> gruposLectura,
        Set<GrupoCampoSensible> gruposEscritura
) {
    public PermisoRecurso {
        DomainAssert.notNull(recurso, "recurso");
        DomainAssert.notNull(alcance, "alcance");
        if (!recurso.supportsScope(alcance)) {
            throw new IllegalArgumentException("Scope " + alcance + " is not supported for " + recurso);
        }
        acciones = immutableEnumSet(acciones, AccionCrm.class);
        idsPermitidos = idsPermitidos == null ? Set.of() : Set.copyOf(idsPermitidos);
        gruposLectura = immutableEnumSet(gruposLectura, GrupoCampoSensible.class);
        gruposEscritura = immutableEnumSet(gruposEscritura, GrupoCampoSensible.class);

        if (!gruposLectura.containsAll(gruposEscritura)) {
            throw new IllegalArgumentException("Sensitive fields cannot be writable unless they are readable");
        }
        if (alcance == AlcanceCrm.TABLEROS_PERMITIDOS && idsPermitidos.isEmpty()) {
            throw new IllegalArgumentException("TABLEROS_PERMITIDOS requires at least one allowed board ID");
        }
        if (alcance != AlcanceCrm.TABLEROS_PERMITIDOS && !idsPermitidos.isEmpty()) {
            throw new IllegalArgumentException("Allowed IDs are only valid for TABLEROS_PERMITIDOS scope");
        }
    }

    public boolean permite(AccionCrm accion) {
        Objects.requireNonNull(accion, "accion");
        return acciones.contains(accion) || acciones.contains(AccionCrm.ADMINISTRAR);
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
