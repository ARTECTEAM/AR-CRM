package com.ar.crm2.application.agenda.service;

import com.ar.crm2.application.agenda.command.EditAgendaCommand;
import com.ar.crm2.application.agenda.exception.AgendaNotFoundException;
import com.ar.crm2.application.agenda.port.in.EditAgendaUseCase;
import com.ar.crm2.application.agenda.port.out.FindAgendaByIdPort;
import com.ar.crm2.application.agenda.port.out.SaveAgendaPort;
import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.application.security.ResourceScopeCandidate;
import com.ar.crm2.application.tarea.exception.TareaNotFoundException;
import com.ar.crm2.application.tarea.port.out.FindTareaByIdPort;
import com.ar.crm2.application.trato.exception.TratoNotFoundException;
import com.ar.crm2.application.trato.port.out.FindTratoByIdPort;
import com.ar.crm2.model.entity.Agenda;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.enums.RecordatorioEstado;
import com.ar.crm2.model.vo.AgendaId;
import com.ar.crm2.model.vo.TareaId;
import com.ar.crm2.model.vo.TratoId;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@RequiredArgsConstructor
public class EditAgendaService implements EditAgendaUseCase {

    private final FindAgendaByIdPort findPort;
    private final SaveAgendaPort savePort;
    private final FindTareaByIdPort findTareaByIdPort;
    private final FindTratoByIdPort findTratoByIdPort;
    private final CrmAuthorization authorization;

    @Override
    public Agenda edit(EditAgendaCommand command) {
        authorization.require(RecursoCrm.AGENDA, AccionCrm.ACTUALIZAR);
        authorization.requireRecord(RecursoCrm.AGENDA, AccionCrm.ACTUALIZAR, command.id());
        AgendaId agendaId = AgendaId.from(command.id());

        Agenda existing = findPort.findById(agendaId)
                .orElseThrow(() -> AgendaNotFoundException.forId(command.id()));

        UUID resolvedTareaId = command.tareaId() != null ? command.tareaId()
                : existing.getTareaId() == null ? null : existing.getTareaId().value();
        UUID resolvedTratoId = command.tratoId() != null ? command.tratoId()
                : existing.getTratoId() == null ? null : existing.getTratoId().value();

        if (resolvedTareaId != null) {
            authorization.requireRecord(RecursoCrm.TAREA, AccionCrm.LEER, resolvedTareaId);
            TareaId tareaId = TareaId.from(resolvedTareaId);
            findTareaByIdPort.findById(tareaId)
                    .orElseThrow(() -> TareaNotFoundException.forId(resolvedTareaId));
        }

        if (resolvedTratoId != null) {
            authorization.requireRecord(RecursoCrm.TRATO, AccionCrm.LEER, resolvedTratoId);
            TratoId tratoId = TratoId.from(resolvedTratoId);
            findTratoByIdPort.findById(tratoId)
                    .orElseThrow(() -> TratoNotFoundException.forId(resolvedTratoId));
        }

        authorization.requireCandidate(RecursoCrm.AGENDA, AccionCrm.ACTUALIZAR,
                new ResourceScopeCandidate(null, null, null,
                        existing.getCreadoPor() == null ? null : existing.getCreadoPor().value()));
        RecordatorioEstado estadoAnterior = existing.getRecordatorioEstado();
        boolean yaEnviado = estadoAnterior == RecordatorioEstado.ENVIADO;
        boolean reminderEnabled = command.recordatorioHabilitado() != null
                ? command.recordatorioHabilitado()
                : existing.isRecordatorioHabilitado();
        Integer reminderMinutes = reminderEnabled
                ? (command.minutosAntes() != null ? command.minutosAntes() : existing.getMinutosAntes())
                : null;
        if (command.minutosAntes() != null && !reminderEnabled) {
            throw new IllegalArgumentException("minutosAntes requires recordatorioHabilitado=true");
        }
        if (reminderEnabled && (reminderMinutes == null || reminderMinutes <= 0)) {
            throw new IllegalArgumentException("minutosAntes must be greater than 0 when reminder is enabled");
        }

        Agenda updated = Agenda.reconstitute(
                existing.getId(),
                command.tipo(),
                command.asunto(),
                command.descripcion() != null ? command.descripcion() : existing.getDescripcion(),
                command.fecha(),
                command.horaInicio(),
                command.horaFin() != null ? command.horaFin() : existing.getHoraFin(),
                resolvedTareaId != null ? TareaId.from(resolvedTareaId) : null,
                resolvedTratoId != null ? TratoId.from(resolvedTratoId) : null,
                command.ubicacion() != null ? command.ubicacion() : existing.getUbicacion(),
                command.linkVideollamada() != null ? command.linkVideollamada() : existing.getLinkVideollamada(),
                existing.getCreadoPor(),
                existing.getCreadoEn(),
                LocalDateTime.now(),
                reminderEnabled,
                reminderMinutes,
                yaEnviado ? RecordatorioEstado.ENVIADO :
                        (reminderEnabled ? RecordatorioEstado.PENDIENTE : null),
                yaEnviado ? existing.getRecordatorioEnviadoEn() : null,
                yaEnviado ? existing.getUltimoIntentoEn() : null
        );

        return savePort.save(updated);
    }
}
