package com.ar.crm2.application.usuario.command;

import java.util.UUID;

/**
 * Command to edit an existing Usuario.
 * Validates id and fields at construction time.
 * Optional rolId is treated as an explicit role reassignment; absent rolId preserves the current role.
 * keycloakId is optional — if present it must match the existing immutable Keycloak linkage.
 */
public record EditUsuarioCommand(
    UUID id,
    String nombre,
    String correo,
    UUID rolId,
    String keycloakId
) {

    public EditUsuarioCommand {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("nombre is required");
        }
        if (correo == null || correo.isBlank()) {
            throw new IllegalArgumentException("correo is required");
        }
    }
}
