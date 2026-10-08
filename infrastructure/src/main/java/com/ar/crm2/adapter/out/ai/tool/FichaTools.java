package com.ar.crm2.adapter.out.ai.tool;

import com.ar.crm2.adapter.out.ai.tool.dto.output.ResourceToolOutput;
import com.ar.crm2.adapter.out.ai.tool.dto.output.FichaOutput;
import com.ar.crm2.adapter.out.ai.tool.dto.output.FichasOutput;
import com.ar.crm2.application.ficha.command.DeleteFichaCommand;
import com.ar.crm2.application.ficha.port.in.CreateFichaUseCase;
import com.ar.crm2.application.ficha.port.in.DeleteFichaUseCase;
import com.ar.crm2.application.ficha.port.in.EditFichaUseCase;
import com.ar.crm2.application.ficha.port.in.GetAllFichasUseCase;
import com.ar.crm2.application.ficha.port.in.GetFichaByIdUseCase;
import com.ar.crm2.application.ficha.port.in.MoverColumnaFichaUseCase;
import com.ar.crm2.model.enums.TipoFicha;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.List;
import java.util.UUID;

/** Spring AI tools for the Ficha REST resource. Actor context is not target ownership authorization. */
@RequiredArgsConstructor
public class FichaTools {

    private final CreateFichaUseCase createFichaUseCase;
    private final GetAllFichasUseCase getAllFichasUseCase;
    private final GetFichaByIdUseCase getFichaByIdUseCase;
    private final EditFichaUseCase editFichaUseCase;
    private final DeleteFichaUseCase deleteFichaUseCase;
    private final MoverColumnaFichaUseCase moverColumnaFichaUseCase;

    @Tool(name = "list_fichas", description = "List CRM cards in stable ID order with bounded output. Actor context is implicit.")
    public FichasOutput listFichas(ToolContext toolContext) {
        ToolContextSupport.requireActor(toolContext);
        return CrmToolMapper.toFichasOutput(getAllFichasUseCase.getAll());
    }

    @Tool(name = "get_ficha", description = "Get a CRM card by ID. Its etiquetaIds output may be truncated; never treat an incomplete label list as the complete set for replacement. Actor context is implicit.")
    public FichaOutput getFicha(@ToolParam(description = "Card UUID.") UUID id, ToolContext toolContext) {
        ToolContextSupport.requireActor(toolContext);
        return CrmToolMapper.toFichaOutput(getFichaByIdUseCase.getById(CrmToolMapper.toGetFichaByIdCommand(id)));
    }

    @Tool(name = "create_ficha", description = "Create a CRM card. etiquetaIds is an optional initial complete label set; ask instead of inventing related UUIDs.")
    public FichaOutput createFicha(
            @ToolParam(description = "Column UUID.") UUID columnaId,
            @ToolParam(description = "Card type (TAREA or TRATO).") TipoFicha tipoFicha,
            @ToolParam(required = false, description = "Deal UUID for TRATO cards; omit when not supplied.") UUID tratoId,
            @ToolParam(required = false, description = "Task UUID for TAREA cards; omit when not supplied.") UUID tareaId,
            @ToolParam(required = false, description = "Optional initial complete set of label UUIDs; omit when no labels were specified.") List<UUID> etiquetaIds,
            ToolContext toolContext) {
        ToolContextSupport.requireActor(toolContext);
        return CrmToolMapper.toFichaOutput(createFichaUseCase.create(
                CrmToolMapper.toCreateFichaCommand(columnaId, tipoFicha, tratoId, tareaId, etiquetaIds)));
    }

    @Tool(name = "edit_ficha", description = "Edit a CRM card and replace its complete label set. Recover unchanged labels using get_ficha or ask the owner; its output may be truncated, so never rebuild from an incomplete label list. etiquetaIds is required; [] clears all labels.")
    public FichaOutput editFicha(
            @ToolParam(description = "Card UUID.") UUID id,
            @ToolParam(description = "Column UUID.") UUID columnaId,
            @ToolParam(description = "Card type (TAREA or TRATO).") TipoFicha tipoFicha,
            @ToolParam(required = false, description = "Deal UUID for TRATO cards; omit when not supplied.") UUID tratoId,
            @ToolParam(required = false, description = "Task UUID for TAREA cards; omit when not supplied.") UUID tareaId,
            @ToolParam(description = "Complete replacement set of label UUIDs; [] explicitly clears all labels.") List<UUID> etiquetaIds,
            ToolContext toolContext) {
        ToolContextSupport.requireActor(toolContext);
        return CrmToolMapper.toFichaOutput(editFichaUseCase.edit(
                CrmToolMapper.toEditFichaCommand(id, columnaId, tipoFicha, tratoId, tareaId, etiquetaIds)));
    }

    @Tool(name = "delete_ficha", description = "Delete a CRM card. This is destructive; call only when the user clearly requested the deletion. Actor context is implicit.")
    public ResourceToolOutput.DeleteResult deleteFicha(
            @ToolParam(description = "Card UUID to delete.") UUID id,
            ToolContext toolContext) {
        ToolContextSupport.requireActor(toolContext);
        deleteFichaUseCase.delete(new DeleteFichaCommand(id));
        return new ResourceToolOutput.DeleteResult("ficha", id.toString(), true);
    }

    @Tool(name = "move_ficha_to_columna", description = "Move a CRM card to another column. Do not infer or fabricate the target UUID. Actor context is implicit.")
    public FichaOutput moveFichaToColumna(
            @ToolParam(description = "Card UUID.") UUID fichaId,
            @ToolParam(description = "Target column UUID supplied by the user or found in CRM data.") UUID targetColumnaId,
            ToolContext toolContext) {
        ToolContextSupport.requireActor(toolContext);
        return CrmToolMapper.toFichaOutput(moverColumnaFichaUseCase.moverAColumna(
                CrmToolMapper.toMoverColumnaFichaCommand(fichaId, targetColumnaId)));
    }
}
