package com.ar.crm2.application.tablero.port.in;

import com.ar.crm2.model.entity.Tablero;

import java.util.List;

/**
 * Inbound input port for retrieving all Tableros.
 */
public interface GetAllTablerosUseCase {

    /**
     * Retrieves all existing Tableros.
     *
     * @return list of all Tableros
     */
    List<Tablero> getAll();
}
