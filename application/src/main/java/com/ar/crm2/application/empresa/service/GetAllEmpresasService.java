package com.ar.crm2.application.empresa.service;

import com.ar.crm2.application.empresa.port.out.FindAllEmpresasPort;
import com.ar.crm2.application.empresa.port.in.GetAllEmpresasUseCase;
import com.ar.crm2.application.empresa.query.EmpresaFilterCriteria;
import com.ar.crm2.application.shared.query.PagedResult;
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

    @Override
    public List<Empresa> getAll(EmpresaFilterCriteria criteria) {
        return findAllPort.findAll(criteria);
    }

    @Override
    public PagedResult<Empresa> getPage(EmpresaFilterCriteria criteria) {
        return findAllPort.findPage(criteria);
    }
}
