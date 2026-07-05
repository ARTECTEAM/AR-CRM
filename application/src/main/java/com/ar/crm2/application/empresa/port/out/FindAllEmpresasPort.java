package com.ar.crm2.application.empresa.port.out;

import com.ar.crm2.application.empresa.query.EmpresaFilterCriteria;
import com.ar.crm2.model.entity.Empresa;

import java.util.List;

/**
 * Granular outbound port for retrieving all Empresas.
 * Single-method contract per project rules.
 * Implementation belongs to infrastructure; contract lives in application.
 */
public interface FindAllEmpresasPort {

    /**
     * Retrieves all Empresas.
     *
     * @return list of all domain entities
     */
    List<Empresa> findAll();

    default List<Empresa> findAll(EmpresaFilterCriteria criteria) {
        return findAll();
    }
}
