package com.ar.crm2.application.tarea.query;

import com.ar.crm2.model.enums.PrioridadTarea;
import com.ar.crm2.model.enums.TipoTarea;
import com.ar.crm2.model.vo.TratoId;
import com.ar.crm2.model.vo.UsuarioId;

/** Criteria owned by application for listing Tareas. */
public record TareaFilterCriteria(
        String search,
        PrioridadTarea prioridad,
        UsuarioId responsableId,
        TratoId tratoId,
        TipoTarea tipo,
        VencimientoFilter vencimiento
) {
    public enum VencimientoFilter { VENCIDAS, PROXIMAS }

    public static TareaFilterCriteria empty() {
        return new TareaFilterCriteria(null, null, null, null, null, null);
    }
}
