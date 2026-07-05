package com.ar.crm2.application.tarea.service;

import com.ar.crm2.application.tarea.port.out.FindAllTareasPort;
import com.ar.crm2.application.tarea.port.in.GetAllTareasUseCase;
import com.ar.crm2.application.tarea.query.TareaFilterCriteria;
import com.ar.crm2.model.entity.Tarea;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Application service implementing GetAllTareasUseCase.
 * Delegates listing directly to the outbound FindAllTareasPort.
 * No Spring annotations — constructor injection via Lombok.
 */
@RequiredArgsConstructor
public class GetAllTareasService implements GetAllTareasUseCase {

    private final FindAllTareasPort findAllPort;

    @Override
    public List<Tarea> getAll(TareaFilterCriteria criteria) {
        return findAllPort.findAll(criteria);
    }
}
