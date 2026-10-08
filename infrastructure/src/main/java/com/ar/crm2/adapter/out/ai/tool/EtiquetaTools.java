package com.ar.crm2.adapter.out.ai.tool;

import com.ar.crm2.adapter.out.ai.tool.dto.output.ResourceToolOutput;
import com.ar.crm2.application.etiqueta.command.CreateEtiquetaCommand;
import com.ar.crm2.application.etiqueta.command.DeleteEtiquetaCommand;
import com.ar.crm2.application.etiqueta.command.EditEtiquetaCommand;
import com.ar.crm2.application.etiqueta.command.GetEtiquetaByIdCommand;
import com.ar.crm2.application.etiqueta.port.in.CreateEtiquetaUseCase;
import com.ar.crm2.application.etiqueta.port.in.DeleteEtiquetaUseCase;
import com.ar.crm2.application.etiqueta.port.in.EditEtiquetaUseCase;
import com.ar.crm2.application.etiqueta.port.in.GetAllEtiquetasUseCase;
import com.ar.crm2.application.etiqueta.port.in.GetEtiquetaByIdUseCase;
import com.ar.crm2.model.enums.TipoEtiqueta;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.Optional;
import java.util.UUID;

/** Spring AI tools for the Etiqueta REST resource. */
@RequiredArgsConstructor
public class EtiquetaTools {

    private final CreateEtiquetaUseCase createEtiquetaUseCase;
    private final GetAllEtiquetasUseCase getAllEtiquetasUseCase;
    private final GetEtiquetaByIdUseCase getEtiquetaByIdUseCase;
    private final EditEtiquetaUseCase editEtiquetaUseCase;
    private final DeleteEtiquetaUseCase deleteEtiquetaUseCase;

    @Tool(name = "create_etiqueta", description = "Create a catalog label. Name, type, and color are required; ask about missing data rather than inventing it. Actor context is implicit.")
    public ResourceToolOutput.Label createEtiqueta(
            @ToolParam(description = "Label name supplied by the user.") String nombre,
            @ToolParam(description = "Label type selected by the user.") TipoEtiqueta tipoEtiqueta,
            @ToolParam(description = "Hex color (#RRGGBB) supplied or explicitly chosen with permission.") String color,
            ToolContext toolContext) {
        ToolContextSupport.requireActor(toolContext);
        return CrmToolMapper.toLabelOutput(createEtiquetaUseCase.create(new CreateEtiquetaCommand(
                CrmToolMapper.requireNonBlank(nombre, "create_etiqueta requires nombre"),
                CrmToolMapper.requireEnum(tipoEtiqueta, "tipoEtiqueta"),
                CrmToolMapper.requireNonBlank(color, "create_etiqueta requires color"))));
    }

    @Tool(name = "list_etiquetas", description = "List catalog labels in stable ID order with bounded output; optionally filter by label type. Actor context is implicit.")
    public ResourceToolOutput.Labels listEtiquetas(
            @ToolParam(required = false, description = "Optional exact label type filter; omit for all labels.") TipoEtiqueta tipoEtiqueta,
            ToolContext toolContext) {
        ToolContextSupport.requireActor(toolContext);
        return CrmToolMapper.toLabelsOutput(getAllEtiquetasUseCase.getAll(Optional.ofNullable(tipoEtiqueta)));
    }

    @Tool(name = "get_etiqueta", description = "Get a catalog label by UUID. Actor context is implicit.")
    public ResourceToolOutput.Label getEtiqueta(
            @ToolParam(description = "Label UUID.") UUID id,
            ToolContext toolContext) {
        ToolContextSupport.requireActor(toolContext);
        return CrmToolMapper.toLabelOutput(getEtiquetaByIdUseCase.getById(new GetEtiquetaByIdCommand(id)));
    }

    @Tool(name = "edit_etiqueta", description = "Edit a label's name and color. Its catalog type is immutable. Ask about missing required values rather than inventing them. Actor context is implicit.")
    public ResourceToolOutput.Label editEtiqueta(
            @ToolParam(description = "Label UUID.") UUID id,
            @ToolParam(description = "New label name.") String nombre,
            @ToolParam(description = "New hex color (#RRGGBB).") String color,
            ToolContext toolContext) {
        ToolContextSupport.requireActor(toolContext);
        return CrmToolMapper.toLabelOutput(editEtiquetaUseCase.edit(new EditEtiquetaCommand(
                id, CrmToolMapper.requireNonBlank(nombre, "edit_etiqueta requires nombre"),
                CrmToolMapper.requireNonBlank(color, "edit_etiqueta requires color"))));
    }

    @Tool(name = "delete_etiqueta", description = "Delete a catalog label. This is destructive and may remove label relations. The optional confirm flag defaults to false; set it true only after the user explicitly confirms deletion. Actor context is implicit.")
    public ResourceToolOutput.DeleteResult deleteEtiqueta(
            @ToolParam(description = "Label UUID to delete.") UUID id,
            @ToolParam(required = false, description = "Explicit user confirmation. Omit or pass false until the user agrees; true is allowed only after explicit agreement.") Boolean confirm,
            ToolContext toolContext) {
        ToolContextSupport.requireActor(toolContext);
        deleteEtiquetaUseCase.delete(new DeleteEtiquetaCommand(id, Boolean.TRUE.equals(confirm)));
        return new ResourceToolOutput.DeleteResult("etiqueta", id.toString(), true);
    }
}
