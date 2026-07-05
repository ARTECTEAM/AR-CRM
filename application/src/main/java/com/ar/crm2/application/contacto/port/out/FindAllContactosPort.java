package com.ar.crm2.application.contacto.port.out;

import com.ar.crm2.application.contacto.query.ContactoFilterCriteria;
import com.ar.crm2.model.entity.Contacto;

import java.util.List;

/**
 * Granular outbound port for retrieving all Contactos.
 * Single-method contract per project rules.
 */
public interface FindAllContactosPort {

    /**
     * Retrieves all Contactos.
     *
     * @return list of all Contacto domain entities
     */
    List<Contacto> findAll();

    default List<Contacto> findAll(ContactoFilterCriteria criteria) {
        return findAll();
    }
}
