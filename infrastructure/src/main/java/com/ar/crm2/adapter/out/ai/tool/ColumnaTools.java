package com.ar.crm2.adapter.out.ai.tool;

import com.ar.crm2.adapter.out.ai.tool.dto.output.ResourceToolOutput;
import com.ar.crm2.adapter.out.ai.tool.dto.output.ColumnaOutput;
import com.ar.crm2.adapter.out.ai.tool.dto.output.ColumnasOutput;
import com.ar.crm2.application.columna.command.DeleteColumnaCommand;
import com.ar.crm2.application.columna.port.in.CreateColumnaUseCase;
import com.ar.crm2.application.columna.port.in.DeleteColumnaUseCase;
import com.ar.crm2.application.columna.port.in.EditColumnaUseCase;
import com.ar.crm2.application.columna.port.in.GetAllColumnasUseCase;
import com.ar.crm2.application.columna.port.in.GetColumnaByIdUseCase;
import com.ar.crm2.model.enums.TipoColumna;
import com.ar.crm2.model.enums.TipoTablero;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.UUID;

/** Spring AI tools for the Columna REST resource. Actor context is not target ownership authorization. */
@RequiredArgsConstructor
public class ColumnaTools {

    private final CreateColumnaUseCase createColumnaUseCase;
    private final GetAllColumnasUseCase getAllColumnasUseCase;
    private final GetColumnaByIdUseCase getColumnaByIdUseCase;
    private final EditColumnaUseCase editColumnaUseCase;
    private final DeleteColumnaUseCase deleteColumnaUseCase;

    @Tool(name = "list_columnas", description = "List CRM columns in stable ID order with bounded output. Actor context is implicit.")
    public ColumnasOutput listColumnas(ToolContext toolContext) {
        ToolContextSupport.requireActor(toolContext);
        return CrmToolMapper.toColumnasOutput(getAllColumnasUseCase.getAll());
    }

    @Tool(name = "get_columna", description = "Get a CRM column by ID. Actor context is implicit.")
    public ColumnaOutput getColumna(@ToolParam(description = "Column UUID.") UUID id, ToolContext toolContext) {
        ToolContextSupport.requireActor(toolContext);
        return CrmToolMapper.toColumnaOutput(getColumnaByIdUseCase.getById(CrmToolMapper.toGetColumnaByIdCommand(id)));
    }

    @Tool(name = "create_columna", description = "Create a CRM column. A trusted super-user claim is used when present and is never a model argument. PREDETERMINADA requires that claim; never substitute another type without permission.")
    public ColumnaOutput createColumna(
            @ToolParam(description = "Column name supplied by the owner.") String nombre,
            @ToolParam(required = false, description = "Optional hex color; omit when not supplied.") String color,
            @ToolParam(description = "Board type (TAREAS or TRATOS), supplied or explicitly authorized by the owner.") TipoTablero tipoTablero,
            @ToolParam(description = "Column type supplied or explicitly authorized by the owner.") TipoColumna tipoColumna,
            ToolContext toolContext) {
        ToolContextSupport.requireActor(toolContext);
        UUID superUsuarioId = ToolContextSupport.trustedOptionalUuid(
                toolContext, ToolContextSupport.SUPER_USUARIO_CONTEXT_KEY);
        if (TipoColumna.PREDETERMINADA == tipoColumna && superUsuarioId == null) {
            throw new SafeToolValidationException("create_columna PREDETERMINADA requires a trusted super-user claim");
        }
        return CrmToolMapper.toColumnaOutput(createColumnaUseCase.create(
                CrmToolMapper.toCreateColumnaCommand(nombre, color, tipoTablero, tipoColumna, superUsuarioId)));
    }

    @Tool(name = "edit_columna", description = "Edit a CRM column. Required name and types must come from the user's request; do not silently change its type. Actor context is implicit.")
    public ColumnaOutput editColumna(
            @ToolParam(description = "Column UUID.") UUID id,
            @ToolParam(description = "Column name.") String nombre,
            @ToolParam(required = false, description = "Optional hex color; omit to preserve the current color.") String color,
            @ToolParam(description = "Board type (TAREAS or TRATOS).") TipoTablero tipoTablero,
            @ToolParam(description = "Column type.") TipoColumna tipoColumna,
            ToolContext toolContext) {
        ToolContextSupport.requireActor(toolContext);
        String effectiveColor = color == null
                ? getColumnaByIdUseCase.getById(CrmToolMapper.toGetColumnaByIdCommand(id)).getColor()
                : color;
        return CrmToolMapper.toColumnaOutput(editColumnaUseCase.edit(
                CrmToolMapper.toEditColumnaCommand(id, nombre, effectiveColor, tipoTablero, tipoColumna)));
    }

    @Tool(name = "delete_columna", description = "Delete a CRM column. This is destructive; call only when the user clearly requested deletion. The application rejects deletion of assigned or in-use columns. Actor context is implicit.")
    public ResourceToolOutput.DeleteResult deleteColumna(
            @ToolParam(description = "Column UUID to delete.") UUID id,
            ToolContext toolContext) {
        ToolContextSupport.requireActor(toolContext);
        deleteColumnaUseCase.delete(new DeleteColumnaCommand(id));
        return new ResourceToolOutput.DeleteResult("columna", id.toString(), true);
    }
}
