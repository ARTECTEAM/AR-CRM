package com.ar.crm2.application.ficha.service;

import com.ar.crm2.application.security.CrmAuthorization;

import com.ar.crm2.application.ficha.port.out.FindAllFichasPort;
import com.ar.crm2.application.ficha.port.in.GetAllFichasUseCase;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Ficha;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Application service implementing GetAllFichasUseCase.
 * Delegates listing directly to the outbound FindAllFichasPort.
 * No Spring annotations — constructor injection via Lombok.
 */
@RequiredArgsConstructor
public class GetAllFichasService implements GetAllFichasUseCase {

    private final CrmAuthorization crmAuthorization;

    private final FindAllFichasPort findAllPort;

    @Override
    public List<Ficha> getAll() {
        crmAuthorization.require(RecursoCrm.FICHA, AccionCrm.LEER);
        // The current unscoped port requires O(n) filtering by board scope and
        // linked-resource access before any raw Ficha reaches an output adapter.
        return findAllPort.findAll().stream()
            .filter(ficha -> crmAuthorization.permitsRecord(
                RecursoCrm.FICHA, AccionCrm.LEER, ficha.getId().value()))
            .filter(ficha -> FichaAccessPolicy.permitsLinkedRecords(crmAuthorization, ficha))
            .toList();
    }
}
