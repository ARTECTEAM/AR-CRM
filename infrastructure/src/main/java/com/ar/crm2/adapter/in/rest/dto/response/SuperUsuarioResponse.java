package com.ar.crm2.adapter.in.rest.dto.response;

import com.ar.crm2.model.entity.SuperUsuario;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

/**
 * REST response DTO for SuperUsuario data.
 * passwordHash is never exposed.
 */
public record SuperUsuarioResponse(
    String id,
    String correo,
    LocalDateTime creadoEn,
    boolean activo,
    @JsonIgnore String keycloakId
) {

    /**
     * Factory method from domain entity.
     */
    public static SuperUsuarioResponse fromDomain(SuperUsuario usuario) {
        return new SuperUsuarioResponse(
            usuario.getId().value().toString(),
            usuario.getCorreo(),
            usuario.getCreadoEn(),
            usuario.isActivo(),
            usuario.getKeycloakId()
        );
    }
}
