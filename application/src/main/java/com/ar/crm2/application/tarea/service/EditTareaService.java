package com.ar.crm2.application.tarea.service;

import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.application.security.ResourceScopeCandidate;

import com.ar.crm2.application.tarea.command.EditTareaCommand;
import com.ar.crm2.application.tarea.exception.TareaNotFoundException;
import com.ar.crm2.application.tarea.port.in.EditTareaUseCase;
import com.ar.crm2.application.tarea.port.out.FindTareaByIdPort;
import com.ar.crm2.application.tarea.port.out.SaveTareaPort;
import com.ar.crm2.model.entity.Tarea;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.vo.TareaId;
import com.ar.crm2.model.vo.UsuarioId;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

/**
 * Application service implementing EditTareaUseCase.
 * Orchestrates loading the aggregate, applying the immutable domain update via reconstitute,
 * and saving. Preserves: id, tratoId, creadoEn, fechaCompletada. A null description is a
 * patch omission; an empty string explicitly clears it.
 */
@RequiredArgsConstructor
public class EditTareaService implements EditTareaUseCase {

    private final CrmAuthorization crmAuthorization;

    private final FindTareaByIdPort findPort;
    private final SaveTareaPort savePort;

    @Override
    public Tarea edit(EditTareaCommand command) {
        TareaId tareaId = TareaId.from(command.id());
        crmAuthorization.requireRecord(RecursoCrm.TAREA, AccionCrm.ACTUALIZAR, command.id());

        Tarea existing = findPort.findById(tareaId)
                .orElseThrow(() -> TareaNotFoundException.forId(command.id()));
        java.util.UUID responsibleId = command.responsableId() != null
                ? command.responsableId() : existing.getResponsableId().value();
        crmAuthorization.requireCandidate(RecursoCrm.TAREA, AccionCrm.ACTUALIZAR,
                new ResourceScopeCandidate(null, null, responsibleId, null));
        // The returned task includes tratoId, so its linked deal must be readable too.
        crmAuthorization.requireRecord(RecursoCrm.TRATO, AccionCrm.LEER, existing.getTratoId().value());

        Tarea updated = Tarea.reconstitute(
                existing.getId(),
                existing.getTratoId(),
                command.responsableId() != null ? UsuarioId.from(command.responsableId()) : null,
                command.titulo(),
                command.descripcion() != null ? command.descripcion() : existing.getDescripcion(),
                command.tipo(),
                command.prioridad(),
                command.fechaLimite(),
                existing.getFechaCompletada(),
                existing.getCreadoEn(),
                LocalDateTime.now()
        );

        return savePort.save(updated);
    }
}
