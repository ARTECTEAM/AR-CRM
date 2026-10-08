package com.ar.crm2.adapter.out.ai.tool;

import com.ar.crm2.adapter.out.ai.tool.dto.output.ResourceToolOutput;
import com.ar.crm2.application.agenda.command.CreateAgendaCommand;
import com.ar.crm2.application.agenda.command.DeleteAgendaCommand;
import com.ar.crm2.application.agenda.command.EditAgendaCommand;
import com.ar.crm2.application.agenda.command.GetAgendaByIdCommand;
import com.ar.crm2.application.agenda.command.GetAgendasByUserCommand;
import com.ar.crm2.application.agenda.port.in.CreateAgendaUseCase;
import com.ar.crm2.application.agenda.port.in.DeleteAgendaUseCase;
import com.ar.crm2.application.agenda.port.in.EditAgendaUseCase;
import com.ar.crm2.application.agenda.port.in.GetAgendaByIdUseCase;
import com.ar.crm2.application.agenda.port.in.GetAgendasByUserUseCase;
import com.ar.crm2.model.enums.TipoAgenda;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/** Spring AI tools for the Agenda REST resource; creator/list identity comes only from trusted context. */
@RequiredArgsConstructor
public class AgendaTools {

    private final CreateAgendaUseCase createAgendaUseCase;
    private final GetAgendasByUserUseCase getAgendasByUserUseCase;
    private final GetAgendaByIdUseCase getAgendaByIdUseCase;
    private final EditAgendaUseCase editAgendaUseCase;
    private final DeleteAgendaUseCase deleteAgendaUseCase;

    @Tool(name = "create_agenda", description = "Create an agenda entry. Required values must be supplied by the user; the creator ID is derived from trusted server context, never a model argument. An omitted reminder-enabled value follows the REST default of false.")
    public ResourceToolOutput.AgendaItem createAgenda(
            @ToolParam(description = "Agenda entry type.") TipoAgenda tipo,
            @ToolParam(description = "Agenda subject; required, non-blank.") String asunto,
            @ToolParam(required = false, description = "Optional description; omit when not supplied.") String descripcion,
            @ToolParam(description = "Agenda date (ISO local date).") LocalDate fecha,
            @ToolParam(description = "Start time (ISO local time).") LocalTime horaInicio,
            @ToolParam(required = false, description = "Optional end time; omit when not supplied.") LocalTime horaFin,
            @ToolParam(required = false, description = "Optional related task UUID; omit when not supplied.") UUID tareaId,
            @ToolParam(required = false, description = "Optional related deal UUID; omit when not supplied.") UUID tratoId,
            @ToolParam(required = false, description = "Optional location; omit when not supplied.") String ubicacion,
            @ToolParam(required = false, description = "Optional meeting URL; omit when not supplied.") String linkVideollamada,
            @ToolParam(required = false, description = "Optional reminder enabled flag; omitted means false, matching REST.") Boolean recordatorioHabilitado,
            @ToolParam(required = false, description = "Optional reminder lead time in minutes; required by the canonical command when reminders are enabled.") Integer minutosAntes,
            ToolContext toolContext) {
        UUID trustedActor = ToolContextSupport.requireActor(toolContext);
        var created = createAgendaUseCase.create(new CreateAgendaCommand(
                CrmToolMapper.requireEnum(tipo, "tipo"),
                CrmToolMapper.requireNonBlank(asunto, "create_agenda requires asunto"), descripcion,
                fecha, horaInicio, horaFin, tareaId, tratoId, ubicacion, linkVideollamada,
                trustedActor, Boolean.TRUE.equals(recordatorioHabilitado), minutosAntes));
        return CrmToolMapper.toAgendaItemOutput(created);
    }

    @Tool(name = "list_agendas", description = "List agenda entries created by the authenticated user. The user ID is taken from trusted server context and is never a model argument.")
    public ResourceToolOutput.AgendaItems listAgendas(ToolContext toolContext) {
        UUID trustedActor = ToolContextSupport.requireActor(toolContext);
        return CrmToolMapper.toAgendaItemsOutput(getAgendasByUserUseCase.getByUser(
                new GetAgendasByUserCommand(trustedActor)));
    }

    @Tool(name = "get_agenda", description = "Get an agenda entry by UUID. Actor context is implicit.")
    public ResourceToolOutput.AgendaItem getAgenda(
            @ToolParam(description = "Agenda entry UUID.") UUID id,
            ToolContext toolContext) {
        ToolContextSupport.requireActor(toolContext);
        return CrmToolMapper.toAgendaItemOutput(getAgendaByIdUseCase.getById(new GetAgendaByIdCommand(id)));
    }

    @Tool(name = "edit_agenda", description = "Edit an agenda entry's editable fields. Ask about missing required values; null/omitted optional values preserve their current values. Empty optional text clears that text; nullable related IDs and end time cannot be cleared through this tool. Actor context is implicit.")
    public ResourceToolOutput.AgendaItem editAgenda(
            @ToolParam(description = "Agenda entry UUID.") UUID id,
            @ToolParam(description = "Agenda entry type.") TipoAgenda tipo,
            @ToolParam(description = "Agenda subject; required, non-blank.") String asunto,
            @ToolParam(required = false, description = "Optional description; omit when not supplied.") String descripcion,
            @ToolParam(description = "Agenda date (ISO local date).") LocalDate fecha,
            @ToolParam(description = "Start time (ISO local time).") LocalTime horaInicio,
            @ToolParam(required = false, description = "Optional end time; omit when not supplied.") LocalTime horaFin,
            @ToolParam(required = false, description = "Optional related task UUID; omit when not supplied.") UUID tareaId,
            @ToolParam(required = false, description = "Optional related deal UUID; omit when not supplied.") UUID tratoId,
            @ToolParam(required = false, description = "Optional location; omit when not supplied.") String ubicacion,
            @ToolParam(required = false, description = "Optional meeting URL; omit when not supplied.") String linkVideollamada,
            @ToolParam(required = false, description = "Optional reminder enabled flag; null preserves the current value.") Boolean recordatorioHabilitado,
            @ToolParam(required = false, description = "Optional reminder lead time in minutes; null preserves the current value while reminders remain enabled.") Integer minutosAntes,
        ToolContext toolContext) {
        ToolContextSupport.requireActor(toolContext);
        if (id == null || fecha == null || horaInicio == null) {
            throw new SafeToolValidationException("edit_agenda requires id, fecha, and horaInicio");
        }
        TipoAgenda resolvedTipo = CrmToolMapper.requireEnum(tipo, "tipo");
        String resolvedAsunto = CrmToolMapper.requireNonBlank(asunto, "edit_agenda requires asunto");
        var current = getAgendaByIdUseCase.getById(new GetAgendaByIdCommand(id));
        if (current == null) {
            throw new SafeToolValidationException("edit_agenda requires an existing agenda entry");
        }
        boolean resolvedRecordatorio = recordatorioHabilitado != null
                ? recordatorioHabilitado : current.isRecordatorioHabilitado();
        Integer resolvedMinutosAntes = minutosAntes != null
                ? minutosAntes : (resolvedRecordatorio ? current.getMinutosAntes() : null);
        if (resolvedRecordatorio && (resolvedMinutosAntes == null || resolvedMinutosAntes <= 0)) {
            throw new SafeToolValidationException("edit_agenda requires a positive minutosAntes when reminders are enabled");
        }
        if (recordatorioHabilitado == null && minutosAntes != null && !resolvedRecordatorio) {
            throw new SafeToolValidationException("edit_agenda requires recordatorioHabilitado=true when setting minutosAntes on an entry without reminders");
        }
        var updated = editAgendaUseCase.edit(new EditAgendaCommand(
                id, resolvedTipo, resolvedAsunto,
                descripcion != null ? descripcion : current.getDescripcion(),
                fecha, horaInicio,
                horaFin != null ? horaFin : current.getHoraFin(),
                tareaId != null ? tareaId : current.getTareaId() == null ? null : current.getTareaId().value(),
                tratoId != null ? tratoId : current.getTratoId() == null ? null : current.getTratoId().value(),
                ubicacion != null ? ubicacion : current.getUbicacion(),
                linkVideollamada != null ? linkVideollamada : current.getLinkVideollamada(),
                resolvedRecordatorio, resolvedMinutosAntes));
        return CrmToolMapper.toAgendaItemOutput(updated);
    }

    @Tool(name = "delete_agenda", description = "Delete an agenda entry. This is destructive; call only when the user clearly requested deletion. Actor context is implicit.")
    public ResourceToolOutput.DeleteResult deleteAgenda(
            @ToolParam(description = "Agenda entry UUID to delete.") UUID id,
            ToolContext toolContext) {
        ToolContextSupport.requireActor(toolContext);
        deleteAgendaUseCase.delete(new DeleteAgendaCommand(id));
        return new ResourceToolOutput.DeleteResult("agenda", id.toString(), true);
    }
}
