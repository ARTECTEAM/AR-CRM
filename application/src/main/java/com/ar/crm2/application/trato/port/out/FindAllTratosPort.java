package com.ar.crm2.application.trato.port.out;

import com.ar.crm2.application.trato.query.TratoFilterCriteria;
import com.ar.crm2.application.shared.query.PagedResult;
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

    default PagedResult<Trato> findPage(TratoFilterCriteria criteria) {
        List<Trato> items = findAll(criteria);
        int size = items.size();
        return new PagedResult<>(items, size, 0, size, 1, false, false);
    }
}
