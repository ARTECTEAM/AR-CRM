package com.ar.crm2.application.trato.port.in;

import com.ar.crm2.application.trato.query.TratoFilterCriteria;
import com.ar.crm2.model.entity.Trato;

import java.util.List;

/**
 * Inbound input port for retrieving all Tratos.
 */
public interface GetAllTratosUseCase {

    /**
     * Retrieves all existing Tratos.
     *
     * @return list of all Tratos
     */
    default List<Trato> getAll() {
        return getAll(TratoFilterCriteria.empty());
    }

    List<Trato> getAll(TratoFilterCriteria criteria);
}
