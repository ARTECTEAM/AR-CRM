package com.ar.crm2.adapter.out.ai.tool;

import com.ar.crm2.application.columna.port.in.CreateColumnaUseCase;
import com.ar.crm2.application.columna.port.in.EditColumnaUseCase;
import com.ar.crm2.application.columna.port.in.GetAllColumnasUseCase;
import com.ar.crm2.application.columna.port.in.GetColumnaByIdUseCase;
import com.ar.crm2.application.ficha.port.in.CreateFichaUseCase;
import com.ar.crm2.application.ficha.port.in.EditFichaUseCase;
import com.ar.crm2.application.ficha.port.in.GetAllFichasUseCase;
import com.ar.crm2.application.ficha.port.in.GetFichaByIdUseCase;
import com.ar.crm2.application.ficha.port.in.MoverColumnaFichaUseCase;
import com.ar.crm2.application.tablero.port.in.AsignarColumnaTableroUseCase;
import com.ar.crm2.application.tablero.port.in.CreateTableroUseCase;
import com.ar.crm2.application.tablero.port.in.EditTableroUseCase;
import com.ar.crm2.application.tablero.port.in.GetAllTablerosUseCase;
import com.ar.crm2.application.tablero.port.in.GetTableroByIdUseCase;
import com.ar.crm2.application.tablero.port.in.ReordenarColumnasUseCase;
import com.ar.crm2.adapter.out.ai.tool.dto.output.ColumnaOutput;
import com.ar.crm2.adapter.out.ai.tool.dto.output.ColumnasOutput;
import com.ar.crm2.adapter.out.ai.tool.dto.output.FichaOutput;
import com.ar.crm2.adapter.out.ai.tool.dto.output.FichasOutput;
import com.ar.crm2.adapter.out.ai.tool.dto.output.TableroOutput;
import com.ar.crm2.adapter.out.ai.tool.dto.output.TablerosOutput;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Development-only board, column, and card tools. The backing Application
 * contracts are not actor/tenant scoped and their writes do not yet have an
 * atomic durable convergence boundary, so Boot must never register this object
 * unless the explicit development feature flag is enabled.
 */
@RequiredArgsConstructor
public final class SpringAiDevelopmentCrmTools {

    static final String ACTOR_CONTEXT_KEY = "actorUsuarioId";
    static final String SUPER_USUARIO_CONTEXT_KEY = "actorSuperUsuarioId";

    private final GetAllTablerosUseCase getAllTablerosUseCase;
    private final GetTableroByIdUseCase getTableroByIdUseCase;
    private final CreateTableroUseCase createTableroUseCase;
    private final EditTableroUseCase editTableroUseCase;
    private final AsignarColumnaTableroUseCase asignarColumnaTableroUseCase;
    private final ReordenarColumnasUseCase reordenarColumnasUseCase;
    private final GetAllColumnasUseCase getAllColumnasUseCase;
    private final GetColumnaByIdUseCase getColumnaByIdUseCase;
    private final CreateColumnaUseCase createColumnaUseCase;
    private final EditColumnaUseCase editColumnaUseCase;
    private final GetAllFichasUseCase getAllFichasUseCase;
    private final GetFichaByIdUseCase getFichaByIdUseCase;
    private final CreateFichaUseCase createFichaUseCase;
    private final EditFichaUseCase editFichaUseCase;
    private final MoverColumnaFichaUseCase moverColumnaFichaUseCase;
    @Tool(name = "list_tableros", description = "List CRM boards in stable ID order with bounded output. Actor context is trusted and implicit.")
    public TablerosOutput listTableros(ToolContext toolContext) {
        requireActor(toolContext);
        return CrmToolMapper.toTablerosOutput(getAllTablerosUseCase.getAll());
    }

    @Tool(name = "get_tablero", description = "Get a CRM board by ID. Actor context is trusted and implicit.")
    public TableroOutput getTablero(@ToolParam(description = "Board UUID.") UUID id, ToolContext toolContext) {
        requireActor(toolContext);
        return CrmToolMapper.toTableroOutput(
                getTableroByIdUseCase.getById(CrmToolMapper.toGetTableroByIdCommand(id)));
    }

    @Tool(name = "create_tablero", description = "Create a CRM board. The trusted actor is not a model argument.")
    public TableroOutput createTablero(@ToolParam(description = "Board name.") String nombre,
            @ToolParam(description = "Board description.") String descripcion,
            @ToolParam(description = "Board type (TipoTablero name).") String tipoTablero,
            ToolContext toolContext) {
        UUID actor = requireActor(toolContext);
        return CrmToolMapper.toTableroOutput(createTableroUseCase.create(
                CrmToolMapper.toCreateTableroCommand(nombre, descripcion, tipoTablero, actor)));
    }

    @Tool(name = "edit_tablero", description = "Edit a CRM board name and description. Actor context is implicit.")
    public TableroOutput editTablero(@ToolParam(description = "Board UUID.") UUID id,
            @ToolParam(description = "Board name.") String nombre,
            @ToolParam(description = "Board description.") String descripcion,
            ToolContext toolContext) {
        requireActor(toolContext);
        return CrmToolMapper.toTableroOutput(
                editTableroUseCase.edit(CrmToolMapper.toEditTableroCommand(id, nombre, descripcion)));
    }

    @Tool(name = "assign_columna_to_tablero", description = "Assign an existing column to a board. Actor context is implicit.")
    public TableroOutput assignColumnaToTablero(@ToolParam(description = "Board UUID.") UUID tableroId,
            @ToolParam(description = "Existing column UUID.") UUID columnaId,
            @ToolParam(description = "Positive WIP limit.") Integer limiteWip,
            @ToolParam(required = false, description = "Board-specific note.") String nota,
            @ToolParam(description = "Initial estimated total.") BigDecimal totalValorEstimado,
            ToolContext toolContext) {
        requireActor(toolContext);
        return CrmToolMapper.toTableroOutput(asignarColumnaTableroUseCase.asignarColumna(
                CrmToolMapper.toAsignarColumnaTableroCommand(
                        tableroId, columnaId, limiteWip, nota, totalValorEstimado)));
    }

    @Tool(name = "reorder_tablero_columns", description = "Replace the complete board-column order. Actor context is implicit.")
    public TableroOutput reorderTableroColumns(@ToolParam(description = "Board UUID.") UUID tableroId,
            @ToolParam(description = "Complete ordered list of column UUIDs.") List<UUID> nuevoOrden,
            ToolContext toolContext) {
        requireActor(toolContext);
        return CrmToolMapper.toTableroOutput(reordenarColumnasUseCase.reordenar(
                CrmToolMapper.toReordenarColumnasCommand(tableroId, nuevoOrden)));
    }

    @Tool(name = "list_columnas", description = "List CRM columns in stable ID order with bounded output. Actor context is trusted and implicit.")
    public ColumnasOutput listColumnas(ToolContext toolContext) {
        requireActor(toolContext);
        return CrmToolMapper.toColumnasOutput(getAllColumnasUseCase.getAll());
    }

    @Tool(name = "get_columna", description = "Get a CRM column by ID. Actor context is trusted and implicit.")
    public ColumnaOutput getColumna(@ToolParam(description = "Column UUID.") UUID id, ToolContext toolContext) {
        requireActor(toolContext);
        return CrmToolMapper.toColumnaOutput(
                getColumnaByIdUseCase.getById(CrmToolMapper.toGetColumnaByIdCommand(id)));
    }

    @Tool(name = "create_columna", description = "Create a CRM column. A trusted super-user claim is used when present and is never a model argument. PREDETERMINADA requires that claim.")
    public ColumnaOutput createColumna(@ToolParam(description = "Column name.") String nombre,
            @ToolParam(required = false, description = "Hex color.") String color,
            @ToolParam(description = "Board type (TipoTablero name).") String tipoTablero,
            @ToolParam(description = "Column type (TipoColumna name).") String tipoColumna,
            ToolContext toolContext) {
        requireActor(toolContext);
        UUID superUsuarioId = optionalUuid(toolContext, SUPER_USUARIO_CONTEXT_KEY);
        if ("PREDETERMINADA".equals(tipoColumna) && superUsuarioId == null) {
            throw new SafeToolValidationException(
                    "create_columna PREDETERMINADA requires a trusted super-user claim");
        }
        return CrmToolMapper.toColumnaOutput(createColumnaUseCase.create(
                CrmToolMapper.toCreateColumnaCommand(
                        nombre, color, tipoTablero, tipoColumna, superUsuarioId)));
    }

    @Tool(name = "edit_columna", description = "Edit a CRM column. Actor context is trusted and implicit.")
    public ColumnaOutput editColumna(@ToolParam(description = "Column UUID.") UUID id,
            @ToolParam(description = "Column name.") String nombre,
            @ToolParam(required = false, description = "Hex color.") String color,
            @ToolParam(description = "Board type (TipoTablero name).") String tipoTablero,
            @ToolParam(description = "Column type (TipoColumna name).") String tipoColumna,
            ToolContext toolContext) {
        requireActor(toolContext);
        return CrmToolMapper.toColumnaOutput(editColumnaUseCase.edit(
                CrmToolMapper.toEditColumnaCommand(id, nombre, color, tipoTablero, tipoColumna)));
    }

    @Tool(name = "list_fichas", description = "List CRM cards in stable ID order with bounded output. Actor context is trusted and implicit.")
    public FichasOutput listFichas(ToolContext toolContext) {
        requireActor(toolContext);
        return CrmToolMapper.toFichasOutput(getAllFichasUseCase.getAll());
    }

    @Tool(name = "get_ficha", description = "Get a CRM card by ID. Actor context is trusted and implicit.")
    public FichaOutput getFicha(@ToolParam(description = "Card UUID.") UUID id, ToolContext toolContext) {
        requireActor(toolContext);
        return CrmToolMapper.toFichaOutput(
                getFichaByIdUseCase.getById(CrmToolMapper.toGetFichaByIdCommand(id)));
    }

    @Tool(name = "create_ficha", description = "Create a CRM card. etiquetaIds is an optional initial complete label set.")
    public FichaOutput createFicha(@ToolParam(description = "Column UUID.") UUID columnaId,
            @ToolParam(description = "Card type (TipoFicha name).") String tipoFicha,
            @ToolParam(required = false, description = "Deal UUID for TRATO cards.") UUID tratoId,
            @ToolParam(required = false, description = "Task UUID for TAREA cards.") UUID tareaId,
            @ToolParam(required = false, description = "Initial complete Etiqueta UUID set.") List<UUID> etiquetaIds,
            ToolContext toolContext) {
        requireActor(toolContext);
        return CrmToolMapper.toFichaOutput(createFichaUseCase.create(
                CrmToolMapper.toCreateFichaCommand(columnaId, tipoFicha, tratoId, tareaId, etiquetaIds)));
    }

    @Tool(name = "edit_ficha", description = "Edit a CRM card and replace its complete label set. etiquetaIds is required; [] clears all labels.")
    public FichaOutput editFicha(@ToolParam(description = "Card UUID.") UUID id,
            @ToolParam(description = "Column UUID.") UUID columnaId,
            @ToolParam(description = "Card type (TipoFicha name).") String tipoFicha,
            @ToolParam(required = false, description = "Deal UUID for TRATO cards.") UUID tratoId,
            @ToolParam(required = false, description = "Task UUID for TAREA cards.") UUID tareaId,
            @ToolParam(description = "Complete replacement Etiqueta UUID set; [] clears all labels.") List<UUID> etiquetaIds,
            ToolContext toolContext) {
        requireActor(toolContext);
        return CrmToolMapper.toFichaOutput(editFichaUseCase.edit(
                CrmToolMapper.toEditFichaCommand(id, columnaId, tipoFicha, tratoId, tareaId, etiquetaIds)));
    }

    @Tool(name = "move_ficha_to_columna", description = "Move a CRM card to another column. Actor context is implicit.")
    public FichaOutput moveFichaToColumna(@ToolParam(description = "Card UUID.") UUID fichaId,
            @ToolParam(description = "Target column UUID.") UUID targetColumnaId,
            ToolContext toolContext) {
        requireActor(toolContext);
        return CrmToolMapper.toFichaOutput(moverColumnaFichaUseCase.moverAColumna(
                CrmToolMapper.toMoverColumnaFichaCommand(fichaId, targetColumnaId)));
    }

    private static UUID requireActor(ToolContext toolContext) {
        UUID actor = optionalUuid(toolContext, ACTOR_CONTEXT_KEY);
        if (actor == null) {
            throw new IllegalStateException("Trusted actor context is required");
        }
        return actor;
    }

    private static UUID optionalUuid(ToolContext toolContext, String key) {
        Map<String, Object> context = toolContext == null ? null : toolContext.getContext();
        if (context == null) {
            return null;
        }
        Object raw = context.get(key);
        if (raw == null) {
            return null;
        }
        if (raw instanceof UUID uuid) {
            return uuid;
        }
        throw new IllegalArgumentException("Trusted identity context has an invalid type");
    }
}
