package com.ar.crm2.application.tarea.port.in;

import com.ar.crm2.application.tarea.query.TareaFilterCriteria;
import com.ar.crm2.model.entity.Tarea;

import java.util.List;

/**
 * Inbound input port for retrieving all Tareas.
 */
public interface GetAllTareasUseCase {

    /**
     * Retrieves all existing Tareas.
     *
     * @return list of all Tareas
     */
    default List<Tarea> getAll() {
        return getAll(TareaFilterCriteria.empty());
    }

    List<Tarea> getAll(TareaFilterCriteria criteria);
}
