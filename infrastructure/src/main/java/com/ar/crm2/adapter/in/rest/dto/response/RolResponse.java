package com.ar.crm2.adapter.in.rest.dto.response;

import com.ar.crm2.model.entity.Rol;
import com.ar.crm2.model.autorizacion.PermisoRecurso;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.AlcanceCrm;
import com.ar.crm2.model.autorizacion.GrupoCampoSensible;
import com.ar.crm2.model.autorizacion.RecursoCrm;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * REST response DTO for Rol.
 * Exposes all fields needed for front-end list/create/edit views.
 */
public record RolResponse(
    UUID id,
    String nombre,
    String descripcion,
    boolean activo,
    List<PermisoRolResponse> permisos
) {
    public record PermisoRolResponse(RecursoCrm recurso, Set<AccionCrm> acciones, AlcanceCrm alcance,
                                     Set<UUID> idsPermitidos,
                                     Set<GrupoCampoSensible> gruposLectura,
                                     Set<GrupoCampoSensible> gruposEscritura) {
        static PermisoRolResponse from(PermisoRecurso grant) {
            return new PermisoRolResponse(grant.recurso(), grant.acciones(), grant.alcance(),
                    grant.idsPermitidos(), grant.gruposLectura(), grant.gruposEscritura());
        }
    }
    /**
     * Maps a domain Rol to this response DTO.
     */
    public static RolResponse fromDomain(Rol rol) {
        return new RolResponse(
            rol.getId().value(),
            rol.getNombre(),
            rol.getDescripcion(),
            rol.isActivo(),
            rol.getPermisos().stream().map(PermisoRolResponse::from).toList()
        );
    }
}
