package com.ar.crm2.application.tablero.service;

import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.application.tablero.port.in.GetAllTablerosUseCase;
import com.ar.crm2.application.tablero.port.out.FindAllTablerosPort;
import com.ar.crm2.application.tablero.query.TableroFilterCriteria;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Tablero;

import java.util.List;

public final class GetAllTablerosService implements GetAllTablerosUseCase {
    private final CrmAuthorization authorization;
    private final FindAllTablerosPort findAllPort;

    public GetAllTablerosService(CrmAuthorization authorization, FindAllTablerosPort findAllPort) {
        this.authorization = authorization;
        this.findAllPort = findAllPort;
    }

    @Override
    public List<Tablero> getAll(TableroFilterCriteria criteria) {
        authorization.require(RecursoCrm.TABLERO, AccionCrm.LEER);
        TableroFilterCriteria resolved = criteria == null ? TableroFilterCriteria.empty() : criteria;
        return findAllPort.findAll(resolved).stream()
                .filter(tablero -> authorization.permitsRecord(
                        RecursoCrm.TABLERO, AccionCrm.LEER, tablero.getId().value()))
                // A board embeds column details and aggregates; omit the aggregate if any assigned column is unreadable.
                .filter(tablero -> tablero.getColumnasTablero().stream().allMatch(column ->
                        authorization.permitsRecord(
                                RecursoCrm.COLUMNA, AccionCrm.LEER, column.getColumnaId().value())))
                .toList();
    }
}