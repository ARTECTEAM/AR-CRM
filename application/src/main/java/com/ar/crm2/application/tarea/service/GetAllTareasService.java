package com.ar.crm2.application.tarea.service;

import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.application.shared.query.AuthorizedListPage;
import com.ar.crm2.application.shared.query.PagedResult;
import com.ar.crm2.application.tarea.port.in.GetAllTareasUseCase;
import com.ar.crm2.application.tarea.port.out.FindAllTareasPort;
import com.ar.crm2.application.tarea.query.TareaFilterCriteria;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Tarea;

import java.util.List;

public final class GetAllTareasService implements GetAllTareasUseCase {
    private final CrmAuthorization authorization;
    private final FindAllTareasPort findAllPort;

    public GetAllTareasService(CrmAuthorization authorization, FindAllTareasPort findAllPort) {
        this.authorization = authorization;
        this.findAllPort = findAllPort;
    }

    @Override
    public List<Tarea> getAll(TareaFilterCriteria criteria) {
        return authorizedRows(criteria);
    }

    @Override
    public PagedResult<Tarea> getPage(TareaFilterCriteria criteria) {
        TareaFilterCriteria resolved = criteria == null ? TareaFilterCriteria.empty() : criteria;
        return AuthorizedListPage.slice(authorizedRows(resolved), resolved.pageRequest());
    }

    private List<Tarea> authorizedRows(TareaFilterCriteria criteria) {
        TareaFilterCriteria resolved = criteria == null ? TareaFilterCriteria.empty() : criteria;
        authorization.require(RecursoCrm.TAREA, AccionCrm.LEER);
        return findAllPort.findAll(resolved).stream()
                .filter(tarea -> authorization.permitsRecord(
                        RecursoCrm.TAREA, AccionCrm.LEER, tarea.getId().value()))
                .filter(tarea -> authorization.permitsRecord(
                        RecursoCrm.TRATO, AccionCrm.LEER, tarea.getTratoId().value()))
                .toList();
    }
}