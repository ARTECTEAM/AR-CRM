package com.ar.crm2.adapter.out.ai.tool;

import com.ar.crm2.adapter.out.ai.tool.dto.output.ResourceToolOutput;
import com.ar.crm2.application.tarea.command.CreateTareaCommand;
import com.ar.crm2.application.tarea.command.DeleteTareaCommand;
import com.ar.crm2.application.tarea.command.EditTareaCommand;
import com.ar.crm2.application.tarea.command.GetTareaByIdCommand;
import com.ar.crm2.application.tarea.port.in.CreateTareaUseCase;
import com.ar.crm2.application.tarea.port.in.DeleteTareaUseCase;
import com.ar.crm2.application.tarea.port.in.EditTareaUseCase;
import com.ar.crm2.application.tarea.port.in.GetAllTareasUseCase;
import com.ar.crm2.application.tarea.port.in.GetTareaByIdUseCase;
import com.ar.crm2.model.enums.PrioridadTarea;
import com.ar.crm2.model.enums.TipoTarea;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.time.LocalDateTime;
import java.util.UUID;

/** Spring AI tools for the Tarea REST resource. Task creation's use case already creates its Ficha. */
@RequiredArgsConstructor
public class TareaTools {

    private final CreateTareaUseCase createTareaUseCase;
    private final GetAllTareasUseCase getAllTareasUseCase;
    private final GetTareaByIdUseCase getTareaByIdUseCase;
    private final EditTareaUseCase editTareaUseCase;
    private final DeleteTareaUseCase deleteTareaUseCase;

    @Tool(name = "create_tarea", description = "Create a task for an existing deal. Required values must be supplied by the user; do not invent related UUIDs or defaults. The canonical use case also creates the task's initial Ficha, so do not create a second Ficha.")
    public ResourceToolOutput.Task createTarea(
            @ToolParam(description = "Existing deal UUID.") UUID tratoId,
            @ToolParam(description = "Business responsible user UUID; not the authenticated actor.") UUID responsableId,
            @ToolParam(description = "Task title; required, non-blank.") String titulo,
            @ToolParam(required = false, description = "Optional task description; omit when not supplied.") String descripcion,
            @ToolParam(description = "Task type.") TipoTarea tipo,
            @ToolParam(description = "Task priority.") PrioridadTarea prioridad,
            @ToolParam(description = "Task due date and time (ISO local date-time).") LocalDateTime fechaLimite,
            ToolContext toolContext) {
        ToolContextSupport.requireActor(toolContext);
        var created = createTareaUseCase.create(new CreateTareaCommand(tratoId, responsableId,
                CrmToolMapper.requireNonBlank(titulo, "create_tarea requires titulo"), descripcion,
                CrmToolMapper.requireEnum(tipo, "tipo"), CrmToolMapper.requireEnum(prioridad, "prioridad"),
                fechaLimite));
        return CrmToolMapper.toTaskOutput(created);
    }

    @Tool(name = "list_tareas", description = "List CRM tasks in stable ID order with bounded output. Actor context is implicit.")
    public ResourceToolOutput.Tasks listTareas(ToolContext toolContext) {
        ToolContextSupport.requireActor(toolContext);
        return CrmToolMapper.toTasksOutput(getAllTareasUseCase.getAll());
    }

    @Tool(name = "get_tarea", description = "Get a CRM task by UUID. Actor context is implicit.")
    public ResourceToolOutput.Task getTarea(
            @ToolParam(description = "Task UUID.") UUID id,
            ToolContext toolContext) {
        ToolContextSupport.requireActor(toolContext);
        return CrmToolMapper.toTaskOutput(getTareaByIdUseCase.getById(new GetTareaByIdCommand(id)));
    }

    @Tool(name = "edit_tarea", description = "Edit a task's editable fields. Its deal link and completion/audit timestamps are preserved. Required values must be supplied; null/omitted description preserves its current value, while an empty string clears it. Actor context is implicit.")
    public ResourceToolOutput.Task editTarea(
            @ToolParam(description = "Task UUID.") UUID id,
            @ToolParam(description = "Business responsible user UUID; not the authenticated actor.") UUID responsableId,
            @ToolParam(description = "Task title; required, non-blank.") String titulo,
            @ToolParam(required = false, description = "Optional task description; omit when not supplied.") String descripcion,
            @ToolParam(description = "Task type.") TipoTarea tipo,
            @ToolParam(description = "Task priority.") PrioridadTarea prioridad,
            @ToolParam(description = "Task due date and time (ISO local date-time).") LocalDateTime fechaLimite,
        ToolContext toolContext) {
        ToolContextSupport.requireActor(toolContext);
        if (id == null || responsableId == null || fechaLimite == null) {
            throw new SafeToolValidationException("edit_tarea requires id, responsableId, and fechaLimite");
        }
        String resolvedTitulo = CrmToolMapper.requireNonBlank(titulo, "edit_tarea requires titulo");
        TipoTarea resolvedTipo = CrmToolMapper.requireEnum(tipo, "tipo");
        PrioridadTarea resolvedPrioridad = CrmToolMapper.requireEnum(prioridad, "prioridad");
        var current = getTareaByIdUseCase.getById(new GetTareaByIdCommand(id));
        if (current == null) {
            throw new SafeToolValidationException("edit_tarea requires an existing task");
        }
        var updated = editTareaUseCase.edit(new EditTareaCommand(id, responsableId,
                resolvedTitulo,
                descripcion != null ? descripcion : current.getDescripcion(),
                resolvedTipo, resolvedPrioridad,
                fechaLimite));
        return CrmToolMapper.toTaskOutput(updated);
    }

    @Tool(name = "delete_tarea", description = "Delete a CRM task. This is destructive; call only when the user clearly requested deletion. Actor context is implicit.")
    public ResourceToolOutput.DeleteResult deleteTarea(
            @ToolParam(description = "Task UUID to delete.") UUID id,
            ToolContext toolContext) {
        ToolContextSupport.requireActor(toolContext);
        deleteTareaUseCase.delete(new DeleteTareaCommand(id));
        return new ResourceToolOutput.DeleteResult("tarea", id.toString(), true);
    }
}
