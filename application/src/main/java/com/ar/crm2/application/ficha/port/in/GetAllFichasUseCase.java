package com.ar.crm2.application.ficha.port.in;

import com.ar.crm2.model.entity.Ficha;

import java.util.List;

/**
 * Inbound input port for retrieving all Fichas.
 */
public interface GetAllFichasUseCase {

    /**
     * Retrieves all existing Fichas.
     *
     * @return list of all Fichas
     */
    List<Ficha> getAll();
}
