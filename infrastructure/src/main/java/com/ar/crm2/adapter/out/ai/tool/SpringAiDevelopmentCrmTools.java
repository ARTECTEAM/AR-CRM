package com.ar.crm2.adapter.out.ai.tool;

import com.ar.crm2.application.columna.port.in.GetAllColumnasUseCase;
import com.ar.crm2.application.columna.port.in.GetColumnaByIdUseCase;
import com.ar.crm2.application.ficha.port.in.GetAllFichasUseCase;
import com.ar.crm2.application.ficha.port.in.GetFichaByIdUseCase;
import com.ar.crm2.application.tablero.port.in.GetAllTablerosUseCase;
import com.ar.crm2.application.tablero.port.in.GetTableroByIdUseCase;
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
    private final GetAllColumnasUseCase getAllColumnasUseCase;
    private final GetColumnaByIdUseCase getColumnaByIdUseCase;
    private final GetAllFichasUseCase getAllFichasUseCase;
    private final GetFichaByIdUseCase getFichaByIdUseCase;
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
