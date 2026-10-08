package com.ar.crm2.model.entity;

import com.ar.crm2.model.vo.RolId;
import com.ar.crm2.model.autorizacion.PermisoRecurso;
import com.ar.crm2.shared.DomainAssert;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

/**
 * Rich domain entity for Rol.
 *
 * Identity: RolId (wraps UUID).
 * Equality: by id only (not full attribute equality).
 * No public setters — state changes go through business methods that preserve invariants.
 * Constructor is private; use static factory methods create() and reconstitute().
 */
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class Rol {

    @EqualsAndHashCode.Include
    private final RolId id;
    private final String nombre;
    private final String descripcion;
    private final boolean activo;
    private final List<PermisoRecurso> permisos;

    // ── Factory ──────────────────────────────────────────────────

    /**
     * Creates a new active Rol.
     * Generates id and sets activo=true.
     */
    public static Rol create(
        String nombre,
        String descripcion
    ) {
        return create(nombre, descripcion, List.of());
    }

    /** Creates a role with the supplied, editable fixed-catalog permissions. */
    public static Rol create(String nombre, String descripcion, List<PermisoRecurso> permisos) {
        DomainAssert.lengthBetween(nombre, "nombre", 1, 80);
        return Rol.builder()
            .id(RolId.create())
            .nombre(nombre.trim())
            .descripcion(descripcion)
            .activo(true)
            .permisos(validatedPermissions(permisos))
            .build();
    }

    /**
     * Reconstitutes an existing Rol from persistence.
     */
    public static Rol reconstitute(
        RolId id,
        String nombre,
        String descripcion,
        boolean activo
    ) {
        return reconstitute(id, nombre, descripcion, activo, List.of());
    }

    /** Reconstitutes a role and its current permission configuration. */
    public static Rol reconstitute(
        RolId id,
        String nombre,
        String descripcion,
        boolean activo,
        List<PermisoRecurso> permisos
    ) {
        DomainAssert.notNull(id, "id");
        DomainAssert.lengthBetween(nombre, "nombre", 1, 80);

        return Rol.builder()
            .id(id)
            .nombre(nombre.trim())
            .descripcion(descripcion)
            .activo(activo)
            .permisos(validatedPermissions(permisos))
            .build();
    }

    /** Returns a copy with newly configured grants, preserving role identity and state. */
    public Rol withPermisos(List<PermisoRecurso> permisos) {
        return Rol.reconstitute(id, nombre, descripcion, activo, permisos);
    }

    private static List<PermisoRecurso> validatedPermissions(List<PermisoRecurso> permissions) {
        List<PermisoRecurso> safe = permissions == null ? List.of() : List.copyOf(permissions);
        if (safe.stream().map(PermisoRecurso::recurso).distinct().count() != safe.size()) {
            throw new IllegalArgumentException("A role may have at most one permission row per resource");
        }
        return safe;
    }

    public boolean isActivo() {
        return activo;
    }
}
