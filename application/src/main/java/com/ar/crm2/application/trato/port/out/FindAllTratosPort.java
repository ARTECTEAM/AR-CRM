package com.ar.crm2.application.trato.port.out;

import com.ar.crm2.application.trato.query.TratoFilterCriteria;
import com.ar.crm2.model.entity.Trato;

import java.util.List;

/**
 * Granular outbound port for retrieving all Tratos.
 * Single-method contract per project rules.
 */
public interface FindAllTratosPort {

    /**
     * Retrieves all Tratos.
     *
     * @return list of all Trato domain entities
     */
    List<Trato> findAll();

    default List<Trato> findAll(TratoFilterCriteria criteria) {
        return findAll();
    }
}
