package com.ar.crm2.adapter.in.rest.mapper;

import com.ar.crm2.adapter.in.rest.dto.request.CreateRolRequest;
import com.ar.crm2.adapter.in.rest.dto.request.EditRolRequest;
import com.ar.crm2.adapter.in.rest.dto.request.PermisoRolRequest;
import com.ar.crm2.application.rol.command.CreateRolCommand;
import com.ar.crm2.application.rol.command.DeleteRolCommand;
import com.ar.crm2.application.rol.command.EditRolCommand;
import com.ar.crm2.application.rol.command.GetRolByIdCommand;
import com.ar.crm2.model.autorizacion.PermisoRecurso;
import com.ar.crm2.model.autorizacion.PlantillaRol;
import com.ar.crm2.model.autorizacion.PlantillasPermisosCrm;

import java.util.UUID;

/**
 * Mapper from REST DTOs to application commands.
 */
public final class RolCommandMapper {

    private RolCommandMapper() {}

    /**
     * Maps a REST create request to an application command.
     */
    public static CreateRolCommand toCommand(CreateRolRequest request) {
        PlantillaRol template = request.plantilla() == null ? PlantillaRol.SIN_ACCESO : request.plantilla();
        java.util.List<PermisoRecurso> permissions = request.permisos() == null
                ? PlantillasPermisosCrm.grantsFor(template)
                : toPermissions(request.permisos());
        return new CreateRolCommand(
            request.nombre(),
            request.descripcion(),
            permissions
        );
    }

    /**
     * Maps an edit request with a query-parameter id to an application command.
     */
    public static EditRolCommand toCommand(UUID id, EditRolRequest request) {
        return new EditRolCommand(
            id,
            request.nombre(),
            request.descripcion(),
            request.activo(),
            request.permisos() == null ? null : toPermissions(request.permisos())
        );
    }

    private static java.util.List<PermisoRecurso> toPermissions(java.util.List<PermisoRolRequest> rows) {
        return rows.stream().map(row -> new PermisoRecurso(row.recurso(), row.acciones(), row.alcance(),
                row.idsPermitidos(), row.gruposLectura(), row.gruposEscritura())).toList();
    }

    /**
     * Maps a query-parameter id to a delete command.
     */
    public static DeleteRolCommand toDeleteCommand(UUID id) {
        return new DeleteRolCommand(id);
    }

    /**
     * Maps a query-parameter id to a get-by-id command.
     */
    public static GetRolByIdCommand toGetByIdCommand(UUID id) {
        return new GetRolByIdCommand(id);
    }
}
