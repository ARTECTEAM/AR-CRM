package com.ar.crm2.adapter.in.rest.mapper;

import com.ar.crm2.adapter.in.rest.dto.request.CreateRolRequest;
import com.ar.crm2.adapter.in.rest.dto.request.EditRolRequest;
import com.ar.crm2.application.rol.command.CreateRolCommand;
import com.ar.crm2.application.rol.command.DeleteRolCommand;
import com.ar.crm2.application.rol.command.EditRolCommand;
import com.ar.crm2.application.rol.command.GetRolByIdCommand;

import java.util.UUID;

/** Mapper from REST DTOs to application commands. */
public final class RolCommandMapper {

    private RolCommandMapper() {}

    /** Maps a REST create request, including its optional permission catalog. */
    public static CreateRolCommand toCommand(CreateRolRequest request) {
        return new CreateRolCommand(
            request.nombre(),
            request.descripcion(),
            request.permisos()
        );
    }

    /** Maps an edit request while preserving null-as-unchanged semantics. */
    public static EditRolCommand toCommand(UUID id, EditRolRequest request) {
        return new EditRolCommand(
            id,
            request.nombre(),
            request.descripcion(),
            request.activo(),
            request.permisos()
        );
    }

    /** Maps a query-parameter id to a delete command. */
    public static DeleteRolCommand toDeleteCommand(UUID id) {
        return new DeleteRolCommand(id);
    }

    /** Maps a query-parameter id to a get-by-id command. */
    public static GetRolByIdCommand toGetByIdCommand(UUID id) {
        return new GetRolByIdCommand(id);
    }
}