package com.ar.crm2.application.tarea.port.out;

import com.ar.crm2.application.tarea.query.TareaFilterCriteria;
import com.ar.crm2.application.shared.query.PagedResult;
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

    default List<Tarea> findAll(TareaFilterCriteria criteria) {
        return findAll();
    }

    default PagedResult<Tarea> findPage(TareaFilterCriteria criteria) {
        List<Tarea> items = findAll(criteria);
        int size = items.size();
        return new PagedResult<>(items, size, 0, size, 1, false, false);
    }
}
