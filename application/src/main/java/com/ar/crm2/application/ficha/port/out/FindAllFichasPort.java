package com.ar.crm2.application.ficha.port.out;

import com.ar.crm2.model.entity.Ficha;

import java.util.List;

/**
 * Granular outbound port for retrieving all Fichas.
 * Single-method contract per project rules.
 */
public interface FindAllFichasPort {

    /**
     * Retrieves all Fichas.
     *
     * @return list of all Ficha domain entities
     */
    List<Ficha> findAll();
}
