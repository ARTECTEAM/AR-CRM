package com.ar.crm2.application.tablero.port.out;

import com.ar.crm2.application.tablero.query.TableroFilterCriteria;
import com.ar.crm2.model.entity.Tablero;

import java.util.List;

/**
 * Granular outbound port for retrieving all Tableros.
 * Single-method contract per project rules.
 */
public interface FindAllTablerosPort {

    /**
     * Returns all persisted Tableros.
     *
     * @return list of all Tablero entities
     */
    List<Tablero> findAll();

    default List<Tablero> findAll(TableroFilterCriteria criteria) {
        return findAll();
    }
}
