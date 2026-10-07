package com.ar.crm2.adapter.out.ai.tool;

import com.ar.crm2.adapter.out.ai.tool.dto.output.ResourceToolOutput;
import com.ar.crm2.adapter.out.ai.tool.dto.output.TableroOutput;
import com.ar.crm2.adapter.out.ai.tool.dto.output.TablerosOutput;
import com.ar.crm2.application.tablero.command.DeleteTableroCommand;
import com.ar.crm2.application.tablero.command.EliminarColumnaDelTableroCommand;
import com.ar.crm2.application.tablero.port.in.AsignarColumnaTableroUseCase;
import com.ar.crm2.application.tablero.port.in.CreateTableroUseCase;
import com.ar.crm2.application.tablero.port.in.DeleteTableroUseCase;
import com.ar.crm2.application.tablero.port.in.EditTableroUseCase;
import com.ar.crm2.application.tablero.port.in.EliminarColumnaDelTableroUseCase;
import com.ar.crm2.application.tablero.port.in.GetAllTablerosUseCase;
import com.ar.crm2.application.tablero.port.in.GetTableroByIdUseCase;
import com.ar.crm2.application.tablero.port.in.ReordenarColumnasUseCase;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.enums.TipoTablero;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Spring AI tools for the Tablero REST resource. Actor context is not target ownership authorization. */
@RequiredArgsConstructor
public class TableroTools {

    private final GetAllTablerosUseCase getAllTablerosUseCase;
    private final GetTableroByIdUseCase getTableroByIdUseCase;
    private final CreateTableroUseCase createTableroUseCase;
    private final EditTableroUseCase editTableroUseCase;
    private final DeleteTableroUseCase deleteTableroUseCase;
    private final EliminarColumnaDelTableroUseCase eliminarColumnaDelTableroUseCase;
    private final AsignarColumnaTableroUseCase asignarColumnaTableroUseCase;
    private final ReordenarColumnasUseCase reordenarColumnasUseCase;

    private final CrmToolOutputProjector outputProjector;

    @Tool(name = "list_tableros", description = "List CRM boards in stable ID order with bounded output. Actor context is implicit.")
    public TablerosOutput listTableros(ToolContext toolContext) {
        ToolContextSupport.requireActor(toolContext);
        return outputProjector.project(CrmToolMapper.toTablerosOutput(getAllTablerosUseCase.getAll()), RecursoCrm.TABLERO);
    }

    @Tool(name = "get_tablero", description = "Get a CRM board by ID. Actor context is implicit.")
    public TableroOutput getTablero(@ToolParam(description = "Board UUID.") UUID id, ToolContext toolContext) {
        ToolContextSupport.requireActor(toolContext);
        return outputProjector.project(CrmToolMapper.toTableroOutput(getTableroByIdUseCase.getById(CrmToolMapper.toGetTableroByIdCommand(id))), RecursoCrm.TABLERO);
    }

    @Tool(name = "create_tablero", description = "Create a CRM board; ask for missing description/type or for permission to choose them. Do not invent required inputs. The board type is TAREAS or TRATOS; never silently default. Actor identity is trusted and is not a model argument.")
    public TableroOutput createTablero(
            @ToolParam(description = "Board name supplied by the owner.") String nombre,
            @ToolParam(description = "Non-blank board description supplied by the owner or chosen with permission.") String descripcion,
            @ToolParam(description = "Board type selected by the owner or with permission (TAREAS or TRATOS). Never silently default.") TipoTablero tipoTablero,
            ToolContext toolContext) {
        UUID actor = ToolContextSupport.requireActor(toolContext);
        return outputProjector.project(CrmToolMapper.toTableroOutput(createTableroUseCase.create(
                CrmToolMapper.toCreateTableroCommand(nombre, descripcion, tipoTablero, actor))), RecursoCrm.TABLERO);
    }

    @Tool(name = "edit_tablero", description = "Edit a CRM board name and description. Actor context is implicit.")
    public TableroOutput editTablero(
            @ToolParam(description = "Board UUID.") UUID id,
            @ToolParam(description = "Board name.") String nombre,
            @ToolParam(description = "Board description.") String descripcion,
            ToolContext toolContext) {
        ToolContextSupport.requireActor(toolContext);
        return outputProjector.project(CrmToolMapper.toTableroOutput(editTableroUseCase.edit(
                CrmToolMapper.toEditTableroCommand(id, nombre, descripcion))), RecursoCrm.TABLERO);
    }

    @Tool(name = "delete_tablero", description = "Delete a CRM board. This is destructive; call only when the user clearly requested the deletion. Actor context is implicit.")
    public ResourceToolOutput.DeleteResult deleteTablero(
            @ToolParam(description = "Board UUID to delete.") UUID id,
            ToolContext toolContext) {
        ToolContextSupport.requireActor(toolContext);
        deleteTableroUseCase.delete(new DeleteTableroCommand(id));
        return new ResourceToolOutput.DeleteResult("tablero", id.toString(), true);
    }

    @Tool(name = "eliminar_columna_del_tablero", description = "Remove a column assignment from a board. This changes board structure; do it only when explicitly requested. Actor context is implicit.")
    public ResourceToolOutput.DeleteResult eliminarColumnaDelTablero(
            @ToolParam(description = "Board UUID.") UUID tableroId,
            @ToolParam(description = "Column UUID assigned to that board.") UUID columnaId,
            ToolContext toolContext) {
        ToolContextSupport.requireActor(toolContext);
        eliminarColumnaDelTableroUseCase.eliminarColumna(
                new EliminarColumnaDelTableroCommand(tableroId, columnaId));
        return new ResourceToolOutput.DeleteResult("tablero_columna", columnaId.toString(), true);
    }

    @Tool(name = "assign_columna_to_tablero", description = "Assign an existing column to a board. Do not invent a WIP limit, note, or initial estimated total; ask when a required value is missing. Actor context is implicit.")
    public TableroOutput assignColumnaToTablero(
            @ToolParam(description = "Board UUID.") UUID tableroId,
            @ToolParam(description = "Existing column UUID.") UUID columnaId,
            @ToolParam(description = "Positive WIP limit supplied by the user.") Integer limiteWip,
            @ToolParam(required = false, description = "Optional board-specific note; omit when not supplied.") String nota,
            @ToolParam(description = "Initial estimated total supplied by the user.") BigDecimal totalValorEstimado,
            ToolContext toolContext) {
        ToolContextSupport.requireActor(toolContext);
        return outputProjector.project(CrmToolMapper.toTableroOutput(asignarColumnaTableroUseCase.asignarColumna(
                CrmToolMapper.toAsignarColumnaTableroCommand(tableroId, columnaId, limiteWip, nota, totalValorEstimado))), RecursoCrm.TABLERO);
    }

    @Tool(name = "reorder_tablero_columns", description = "Replace the complete board-column order. Preserve existing columns unless the owner explicitly requests otherwise; clarify an incomplete order instead of inventing or silently changing it. Actor context is implicit.")
    public TableroOutput reorderTableroColumns(
            @ToolParam(description = "Board UUID.") UUID tableroId,
            @ToolParam(description = "Complete ordered list of the board's column UUIDs in the requested final order.") List<UUID> nuevoOrden,
            ToolContext toolContext) {
        ToolContextSupport.requireActor(toolContext);
        return outputProjector.project(CrmToolMapper.toTableroOutput(reordenarColumnasUseCase.reordenar(
                CrmToolMapper.toReordenarColumnasCommand(tableroId, nuevoOrden))), RecursoCrm.TABLERO);
    }
}
