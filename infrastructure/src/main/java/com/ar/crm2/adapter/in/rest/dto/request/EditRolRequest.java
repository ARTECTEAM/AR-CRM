package com.ar.crm2.adapter.in.rest.dto.request;

import com.ar.crm2.model.autorizacion.PermisoRecurso;
import jakarta.validation.constraints.Size;

import java.util.List;

/** REST request DTO for editing role details and optionally replacing its grants/state. */
public record EditRolRequest(
    @Size(max = 80, message = "nombre must not exceed 80 characters")
    String nombre,

    String descripcion,

    Boolean activo,

    List<PermisoRecurso> permisos
) {
    /** Preserves source compatibility with callers using the legacy two-field request. */
    public EditRolRequest(String nombre, String descripcion) {
        this(nombre, descripcion, null, null);
    }

    public EditRolRequest {
        // Null means leave the existing grants untouched; an empty list explicitly revokes all.
        permisos = permisos == null ? null : List.copyOf(permisos);
    }
}