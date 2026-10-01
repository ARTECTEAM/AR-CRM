package com.ar.crm2.application.tablero.service;

import com.ar.crm2.application.security.CrmAuthorization;

import com.ar.crm2.application.tablero.port.out.FindAllTablerosPort;
import com.ar.crm2.application.tablero.port.in.GetAllTablerosUseCase;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Tablero;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Application service implementing GetAllTablerosUseCase.
 * Delegates listing directly to the outbound FindAllTablerosPort.
 * No Spring annotations — constructor injection via Lombok.
 */
@RequiredArgsConstructor
public class GetAllTablerosService implements GetAllTablerosUseCase {

    private final CrmAuthorization crmAuthorization;

    private final FindAllTablerosPort findAllPort;

    @Override
    public List<Tablero> getAll() {
        crmAuthorization.require(RecursoCrm.TABLERO, AccionCrm.LEER);
        // The current outbound port is unscoped, so filtering costs O(n) until
        // scoped query ports are introduced; only visible rows leave this service.
        return findAllPort.findAll().stream()
            .filter(tablero -> crmAuthorization.permitsRecord(
                RecursoCrm.TABLERO, AccionCrm.LEER, tablero.getId().value()))
            // A board response embeds complete column metadata and financial totals; do not
            // expose a partial raw aggregate when even one assigned column is unreadable.
            .filter(tablero -> tablero.getColumnasTablero().stream().allMatch(column ->
                crmAuthorization.permitsRecord(RecursoCrm.COLUMNA, AccionCrm.LEER,
                    column.getColumnaId().value())))
            .toList();
    }
}
