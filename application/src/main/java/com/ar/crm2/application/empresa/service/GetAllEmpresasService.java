package com.ar.crm2.application.empresa.service;

import com.ar.crm2.application.empresa.port.out.FindAllEmpresasPort;
import com.ar.crm2.application.empresa.port.in.GetAllEmpresasUseCase;
import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Empresa;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Application service implementing GetAllEmpresasUseCase.
 * Delegates listing directly to the outbound FindAllEmpresasPort.
 * No Spring annotations — constructor injection via Lombok.
 */
@RequiredArgsConstructor
public class GetAllEmpresasService implements GetAllEmpresasUseCase {

    private final FindAllEmpresasPort findAllPort;
    private final CrmAuthorization authorization;

    @Override
    public List<Empresa> getAll() {
        authorization.require(RecursoCrm.EMPRESA, AccionCrm.LEER);
        return findAllPort.findAll().stream()
                .filter(empresa -> authorization.permitsRecord(
                        RecursoCrm.EMPRESA, AccionCrm.LEER, empresa.getId().value()))
                .toList();
    }
}
