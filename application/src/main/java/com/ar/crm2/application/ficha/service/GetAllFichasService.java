package com.ar.crm2.application.ficha.service;

import com.ar.crm2.application.ficha.port.in.GetAllFichasUseCase;
import com.ar.crm2.application.ficha.port.out.FindAllFichasPort;
import com.ar.crm2.application.ficha.query.FichaFilterCriteria;
import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Ficha;

import java.util.List;

public final class GetAllFichasService implements GetAllFichasUseCase {
    private final CrmAuthorization authorization;
    private final FindAllFichasPort findAllPort;

    public GetAllFichasService(CrmAuthorization authorization, FindAllFichasPort findAllPort) {
        this.authorization = authorization;
        this.findAllPort = findAllPort;
    }

    @Override
    public List<Ficha> getAll(FichaFilterCriteria criteria) {
        authorization.require(RecursoCrm.FICHA, AccionCrm.LEER);
        FichaFilterCriteria resolved = criteria == null ? FichaFilterCriteria.empty() : criteria;
        return findAllPort.findAll(resolved).stream()
                .filter(ficha -> authorization.permitsRecord(
                        RecursoCrm.FICHA, AccionCrm.LEER, ficha.getId().value()))
                .filter(ficha -> FichaAccessPolicy.permitsLinkedRecords(authorization, ficha))
                .toList();
    }
}