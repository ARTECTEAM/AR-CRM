package com.ar.crm2.adapter.in.rest.dto.request;

import com.ar.crm2.model.autorizacion.PermisoRecurso;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/** REST request DTO for creating a role with optional fixed-catalog grants. */
public record CreateRolRequest(
    @NotBlank(message = "nombre is required")
    @Size(max = 80, message = "nombre must not exceed 80 characters")
    String nombre,

    String descripcion,

    List<PermisoRecurso> permisos
) {
    /** Preserves source compatibility with callers using the legacy two-field request. */
    public CreateRolRequest(String nombre, String descripcion) {
        this(nombre, descripcion, List.of());
    }

    public CreateRolRequest {
        permisos = permisos == null ? List.of() : List.copyOf(permisos);
    }
}