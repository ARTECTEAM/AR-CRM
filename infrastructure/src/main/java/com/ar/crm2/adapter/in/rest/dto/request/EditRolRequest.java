package com.ar.crm2.adapter.in.rest.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * REST request DTO for editing an existing Rol.
 * Missing grant or active fields preserve their current policy state.
 */
public record EditRolRequest(
    @NotBlank(message = "nombre is required")
    @Size(max = 80, message = "nombre must not exceed 80 characters")
    String nombre,

    String descripcion,

    /** Null preserves the current active state. */
    Boolean activo,

    /** Null preserves the current grants; [] explicitly revokes them. */
    @Valid
    List<@NotNull PermisoRolRequest> permisos
) {}
