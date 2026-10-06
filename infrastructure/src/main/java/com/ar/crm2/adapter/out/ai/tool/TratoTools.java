package com.ar.crm2.adapter.out.ai.tool;

import com.ar.crm2.adapter.out.ai.tool.dto.output.ResourceToolOutput;
import com.ar.crm2.adapter.out.ai.tool.dto.output.EditTratoOutput;
import com.ar.crm2.application.trato.command.CreateTratoCommand;
import com.ar.crm2.application.trato.command.DeleteTratoCommand;
import com.ar.crm2.application.trato.command.GetTratoByIdCommand;
import com.ar.crm2.application.trato.port.in.CreateTratoUseCase;
import com.ar.crm2.application.trato.port.in.DeleteTratoUseCase;
import com.ar.crm2.application.trato.port.in.EditTratoUseCase;
import com.ar.crm2.application.trato.port.in.GetAllTratosUseCase;
import com.ar.crm2.application.trato.port.in.GetTratoByIdUseCase;
import com.ar.crm2.model.enums.TipoContrato;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Spring AI tools for the Trato REST resource. Actor context is not target ownership authorization. */
@RequiredArgsConstructor
public class TratoTools {

    private final CreateTratoUseCase createTratoUseCase;
    private final GetAllTratosUseCase getAllTratosUseCase;
    private final GetTratoByIdUseCase getTratoByIdUseCase;
    private final EditTratoUseCase editTratoUseCase;
    private final DeleteTratoUseCase deleteTratoUseCase;

    @Tool(name = "create_trato", description = "Create a deal. Required contact, responsible-user, name, and contract-type choices must be supplied by the user; do not invent UUIDs or defaults. The canonical use case also creates its initial Ficha, so do not create a second card. Actor identity is trusted server context and is not a model argument.")
    public ResourceToolOutput.Deal createTrato(
            @ToolParam(description = "Existing contact UUID.") UUID contactoId,
            @ToolParam(description = "Business responsible user UUID; not the authenticated actor.") UUID responsableId,
            @ToolParam(description = "Deal name; required, non-blank.") String nombre,
            @ToolParam(required = false, description = "Optional estimated deal value; omit when not supplied.") BigDecimal valorEstimado,
            @ToolParam(required = false, description = "Optional win probability from 0 to 100; omit when not supplied.") Integer probabilidad,
            @ToolParam(required = false, description = "Optional expected close date (ISO local date); omit when not supplied.") LocalDate fechaCierreEsperada,
            @ToolParam(description = "Contract type supplied or explicitly authorized by the owner.") TipoContrato tipoContrato,
            ToolContext toolContext) {
        ToolContextSupport.requireActor(toolContext);
        var created = createTratoUseCase.create(new CreateTratoCommand(contactoId, responsableId,
                CrmToolMapper.requireNonBlank(nombre, "create_trato requires nombre"), valorEstimado,
                probabilidad, fechaCierreEsperada, CrmToolMapper.requireEnum(tipoContrato, "tipoContrato")));
        return CrmToolMapper.toDealOutput(created);
    }

    @Tool(name = "list_tratos", description = "List CRM deals in stable ID order with bounded output. Actor context is implicit.")
    public ResourceToolOutput.Deals listTratos(ToolContext toolContext) {
        ToolContextSupport.requireActor(toolContext);
        return CrmToolMapper.toDealsOutput(getAllTratosUseCase.getAll());
    }

    @Tool(name = "get_trato", description = "Get a CRM deal by UUID. Actor context is implicit.")
    public ResourceToolOutput.Deal getTrato(
            @ToolParam(description = "Deal UUID.") UUID id,
            ToolContext toolContext) {
        ToolContextSupport.requireActor(toolContext);
        return CrmToolMapper.toDealOutput(getTratoByIdUseCase.getById(new GetTratoByIdCommand(id)));
    }

    @Tool(
            name = "edit_trato",
            description = "Edit an existing deal's editable business fields (name, estimated value, "
                    + "probability, expected close date, contract type, or responsible user). "
                    + "The deal's stage (estado) is not changed by this tool. "
                    + "responsableId is the business assignee, not the authenticated actor. "
                    + "Contract type is required. Ask about missing required data; null/omitted optional values preserve their current values and cannot be cleared through this tool. Actor context is implicit."
    )
    public EditTratoOutput editTrato(
            @ToolParam(description = "Deal UUID.") UUID id,
            @ToolParam(description = "Business responsible user UUID; not the authenticated actor.") UUID responsableId,
            @ToolParam(description = "Deal name; required, non-blank.") String nombre,
            @ToolParam(required = false, description = "Optional estimated deal value; null preserves the current value.") BigDecimal valorEstimado,
            @ToolParam(required = false, description = "Optional win probability percentage 0-100; null preserves the current value.") Integer probabilidad,
            @ToolParam(required = false, description = "Optional expected close date (ISO local date); null preserves the current value.") LocalDate fechaCierreEsperada,
            @ToolParam(description = "Required contract type, supplied or explicitly authorized by the owner.") TipoContrato tipoContrato,
        ToolContext toolContext) {
        ToolContextSupport.requireActor(toolContext);
        if (id == null || responsableId == null) {
            throw new SafeToolValidationException("edit_trato requires id and responsableId");
        }
        String resolvedNombre = CrmToolMapper.requireNonBlank(nombre, "edit_trato requires nombre");
        TipoContrato resolvedTipoContrato = CrmToolMapper.requireEnum(tipoContrato, "edit_trato tipoContrato");
        var current = getTratoByIdUseCase.getById(new GetTratoByIdCommand(id));
        if (current == null) {
            throw new SafeToolValidationException("edit_trato requires an existing deal");
        }
        var updated = editTratoUseCase.edit(CrmToolMapper.toEditTratoCommand(
                id, responsableId, resolvedNombre,
                valorEstimado != null ? valorEstimado : current.getValorEstimado(),
                probabilidad != null ? probabilidad : current.getProbabilidad(),
                fechaCierreEsperada != null ? fechaCierreEsperada : current.getFechaCierreEsperada(),
                resolvedTipoContrato));
        return CrmToolMapper.toEditTratoOutput(updated);
    }

    @Tool(name = "delete_trato", description = "Delete a CRM deal. This is destructive; call only when the user clearly requested deletion. Actor context is implicit.")
    public ResourceToolOutput.DeleteResult deleteTrato(
            @ToolParam(description = "Deal UUID to delete.") UUID id,
            ToolContext toolContext) {
        ToolContextSupport.requireActor(toolContext);
        deleteTratoUseCase.delete(new DeleteTratoCommand(id));
        return new ResourceToolOutput.DeleteResult("trato", id.toString(), true);
    }
}
