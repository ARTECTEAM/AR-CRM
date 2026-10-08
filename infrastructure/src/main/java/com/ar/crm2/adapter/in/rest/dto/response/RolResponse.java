package com.ar.crm2.adapter.in.rest.dto.response;

import com.ar.crm2.model.autorizacion.PermisoRecurso;
import com.ar.crm2.model.entity.Rol;

import java.util.List;
import java.util.UUID;

/** REST response DTO exposing the configurable role catalog to authorized administrators. */
public record RolResponse(
    UUID id,
    String nombre,
    String descripcion,
    boolean activo,
    List<PermisoRecurso> permisos
) {
    /** Preserves source compatibility with callers using the legacy four-field response. */
    public RolResponse(UUID id, String nombre, String descripcion, boolean activo) {
        this(id, nombre, descripcion, activo, List.of());
    }

    public RolResponse {
        permisos = permisos == null ? List.of() : List.copyOf(permisos);
    }

    /** Maps a domain role and its effective permission catalog to this response DTO. */
    public static RolResponse fromDomain(Rol rol) {
        return new RolResponse(
            rol.getId().value(),
            rol.getNombre(),
            rol.getDescripcion(),
            rol.isActivo(),
            rol.getPermisos()
        );
    }
}