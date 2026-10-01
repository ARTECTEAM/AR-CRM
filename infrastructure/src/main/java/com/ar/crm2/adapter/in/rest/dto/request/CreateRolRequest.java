package com.ar.crm2.adapter.in.rest.dto.request;

import com.ar.crm2.model.autorizacion.PlantillaRol;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * REST request DTO for creating a new Rol.
 * Required fields validated at construction time.
 */
public record CreateRolRequest(
    @NotBlank(message = "nombre is required")
    @Size(max = 80, message = "nombre must not exceed 80 characters")
    String nombre,

    String descripcion,

    /** Optional editable starter preset. Defaults to no access. */
    PlantillaRol plantilla,

    /** Optional explicit policy rows; when supplied these replace the preset. */
    @Valid
    List<@NotNull PermisoRolRequest> permisos
) {}
