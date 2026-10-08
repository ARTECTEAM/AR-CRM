package com.ar.crm2.application.columna.service;

import com.ar.crm2.application.security.CrmAuthorization;

import com.ar.crm2.application.columna.port.in.GetAllColumnasUseCase;
import com.ar.crm2.application.columna.port.out.FindAllColumnasPort;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Columna;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Application service implementing GetAllColumnasUseCase.
 * Delegates listing directly to the outbound FindAllColumnasPort.
 * No Spring annotations — constructor injection via Lombok.
 */
@RequiredArgsConstructor
public class GetAllColumnasService implements GetAllColumnasUseCase {

    private final CrmAuthorization crmAuthorization;

    private final FindAllColumnasPort findAllPort;

    @Override
    public List<Columna> getAll() {
        crmAuthorization.require(RecursoCrm.COLUMNA, AccionCrm.LEER);
        // Catalog columns are currently returned from an unscoped port; this
        // costs O(n) until a scoped query port exists, but no hidden rows escape.
        return findAllPort.findAll().stream()
            .filter(columna -> crmAuthorization.permitsRecord(
                RecursoCrm.COLUMNA, AccionCrm.LEER, columna.getId().value()))
            .toList();
    }
}
