package com.ar.crm2.application.contacto.port.out;

import com.ar.crm2.application.contacto.query.ContactoFilterCriteria;
import com.ar.crm2.application.shared.query.PagedResult;
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

    default PagedResult<Contacto> findPage(ContactoFilterCriteria criteria) {
        List<Contacto> items = findAll(criteria);
        int size = items.size();
        return new PagedResult<>(items, size, 0, size, 1, false, false);
    }
}
