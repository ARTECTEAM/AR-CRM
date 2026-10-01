package com.ar.crm2.application.tarea.service;

import com.ar.crm2.application.security.CrmAuthorization;

import com.ar.crm2.application.tarea.port.out.FindAllTareasPort;
import com.ar.crm2.application.tarea.port.in.GetAllTareasUseCase;
import com.ar.crm2.model.entity.Tarea;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Application service implementing GetAllTareasUseCase.
 * Delegates listing directly to the outbound FindAllTareasPort.
 * No Spring annotations — constructor injection via Lombok.
 */
@RequiredArgsConstructor
public class GetAllTareasService implements GetAllTareasUseCase {

    private final CrmAuthorization crmAuthorization;

    private final FindAllTareasPort findAllPort;

    @Override
    public List<Tarea> getAll() {
        crmAuthorization.require(RecursoCrm.TAREA, AccionCrm.LEER);
        // The current outbound port is unscoped (O(n) filtering); only permitted
        // rows are returned and downstream totals are computed after this filter.
        return findAllPort.findAll().stream()
            .filter(tarea -> crmAuthorization.permitsRecord(
                RecursoCrm.TAREA, AccionCrm.LEER, tarea.getId().value()))
            // A task response contains tratoId, so do not reveal the task or link
            // when its referenced deal is outside the caller's current read scope.
            .filter(tarea -> crmAuthorization.permitsRecord(
                RecursoCrm.TRATO, AccionCrm.LEER, tarea.getTratoId().value()))
            .toList();
    }
}
