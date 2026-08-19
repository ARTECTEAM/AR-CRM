package com.ar.crm2.application.tarea.port.out;

import com.ar.crm2.model.entity.Tarea;

import java.util.List;

/**
 * Granular outbound port for retrieving all Tareas.
 * Single-method contract per project rules.
 */
public interface FindAllTareasPort {

    /**
     * Retrieves all Tareas.
     *
     * @return list of all Tarea domain entities
     */
    List<Tarea> findAll();
}
