package com.ar.crm2.application.empresa.port.in;

import com.ar.crm2.model.entity.Empresa;

import java.util.List;

/**
 * Inbound input port for retrieving all Empresas.
 * UseCase suffix per project rules: inbound contracts live in port/in package.
 */
public interface GetAllEmpresasUseCase {

    /**
     * Retrieves all Empresas for listing.
     *
     * @return list of all domain entities
     */
    List<Empresa> getAll();
}
