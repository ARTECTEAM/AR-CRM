package com.ar.crm2.application.tarea.service;

import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.application.security.ResourceScopeCandidate;

import com.ar.crm2.application.ficha.port.out.SaveFichaPort;
import com.ar.crm2.application.tablero.port.out.FindInitialColumnPort;
import com.ar.crm2.application.tarea.command.CreateTareaCommand;
import com.ar.crm2.application.tarea.port.out.SaveTareaPort;
import com.ar.crm2.application.tarea.port.in.CreateTareaUseCase;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Ficha;
import com.ar.crm2.model.entity.Tarea;
import com.ar.crm2.model.entity.Columna;
import com.ar.crm2.model.enums.TipoFicha;
import com.ar.crm2.model.enums.TipoTablero;
import com.ar.crm2.model.vo.TratoId;
import com.ar.crm2.model.vo.UsuarioId;
import lombok.RequiredArgsConstructor;

/**
 * Application service implementing CreateTareaUseCase.
 * Orchestrates domain entity creation and outbound persistence via SaveTareaPort.
 * Also automatically creates a Kanban Ficha in the initial column of the global TAREA board.
 * No Spring annotations — constructor injection via Lombok.
 */
@RequiredArgsConstructor
public class CreateTareaService implements CreateTareaUseCase {

    private final CrmAuthorization crmAuthorization;

    private final SaveTareaPort savePort;
    private final SaveFichaPort saveFichaPort;
    private final FindInitialColumnPort findInitialColumnPort;

    @Override
    public Tarea create(CreateTareaCommand command) {
        crmAuthorization.require(RecursoCrm.TAREA, AccionCrm.CREAR);
        crmAuthorization.requireCandidate(RecursoCrm.TAREA, AccionCrm.CREAR,
            new ResourceScopeCandidate(null, null, command.responsableId(), null));
        crmAuthorization.requireRecord(RecursoCrm.TRATO, AccionCrm.LEER, command.tratoId());
        crmAuthorization.require(RecursoCrm.FICHA, AccionCrm.CREAR);
        Columna columnaInicial = findInitialColumnPort.findInitialColumn(TipoTablero.TAREAS)
            .orElseThrow(() -> new IllegalStateException(
                "No initial column found for global TAREAS board. Cannot create Tarea without Kanban Ficha."
            ));
        crmAuthorization.requireCandidate(RecursoCrm.FICHA, AccionCrm.CREAR,
            new ResourceScopeCandidate(RecursoCrm.COLUMNA, columnaInicial.getId().value(), null, null));
        crmAuthorization.requireRecord(RecursoCrm.COLUMNA, AccionCrm.LEER, columnaInicial.getId().value());

        Tarea tarea = Tarea.create(
            TratoId.from(command.tratoId()),
            UsuarioId.from(command.responsableId()),
            command.titulo(),
            command.descripcion(),
            command.tipo(),
            command.prioridad(),
            command.fechaLimite()
        );
        Tarea savedTarea = savePort.save(tarea);

        Ficha ficha = Ficha.create(
            columnaInicial.getId(),
            TipoFicha.TAREA,
            null,
            savedTarea.getId()
        );
        saveFichaPort.save(ficha);

        return savedTarea;
    }
}
