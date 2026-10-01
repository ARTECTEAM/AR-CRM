package com.ar.crm2.application.usuario.port.out;

import com.ar.crm2.model.entity.Usuario;

import java.util.UUID;

/** Performs the one-time bootstrap user's manager promotion under the persistence lock. */
public interface PromoteBootstrapAdministratorPort {
    Usuario promoteIfNoActiveManager(Usuario usuario, UUID bootstrapActorUsuarioId);
}
