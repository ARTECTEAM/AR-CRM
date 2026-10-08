package com.ar.crm2.application.rol.command;

import com.ar.crm2.model.autorizacion.PermisoRecurso;
import java.util.List;

/**
 * Command to create a new Rol.
 * Required fields validated at construction time.
 */
public record CreateRolCommand(
    String nombre,
    String descripcion,
    List<PermisoRecurso> permisos
) {

    public CreateRolCommand(String nombre, String descripcion) {
        this(nombre, descripcion, List.of());
    }

    public CreateRolCommand {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("nombre is required");
        }
        permisos = permisos == null ? List.of() : List.copyOf(permisos);
    }
}
