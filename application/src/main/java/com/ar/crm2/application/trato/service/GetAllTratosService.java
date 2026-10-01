package com.ar.crm2.application.trato.service;

import com.ar.crm2.application.security.CrmAuthorization;

import com.ar.crm2.application.trato.port.out.FindAllTratosPort;
import com.ar.crm2.application.trato.port.in.GetAllTratosUseCase;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Trato;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Application service implementing GetAllTratosUseCase.
 * Delegates listing directly to the outbound FindAllTratosPort.
 * No Spring annotations — constructor injection via Lombok.
 */
@RequiredArgsConstructor
public class GetAllTratosService implements GetAllTratosUseCase {

    private final CrmAuthorization crmAuthorization;

    private final FindAllTratosPort findAllPort;

    @Override
    public List<Trato> getAll() {
        crmAuthorization.require(RecursoCrm.TRATO, AccionCrm.LEER);
        // The current outbound port is unscoped (O(n) filtering); only permitted
        // rows are returned and downstream totals are computed after this filter.
        return findAllPort.findAll().stream()
            .filter(trato -> crmAuthorization.permitsRecord(
                RecursoCrm.TRATO, AccionCrm.LEER, trato.getId().value()))
            .filter(trato -> crmAuthorization.permitsRecord(
                RecursoCrm.CONTACTO, AccionCrm.LEER, trato.getContactoId().value()))
            .toList();
    }
}
